'use strict';

const vscode = require('vscode');
const path = require('path');
const fs = require('fs');
const { lintNassi } = require('./lint');

/**
 * Scans a single line, starting from the given brace `depth` and paragraph
 * state, and returns the resulting state at the end of the line.
 *
 * Braces (`{` / `}`) are only structural when they appear outside of a `"""`
 * paragraph. nassi SENTENCEs cannot contain braces (the grammar forbids them),
 * so the only place a non-structural brace can occur is inside a paragraph.
 *
 * @param {string} s
 * @param {number} depth
 * @param {boolean} inPara
 * @returns {{ depth: number, inPara: boolean }}
 */
function scanLine(s, depth, inPara) {
  let i = 0;
  while (i < s.length) {
    if (inPara) {
      if (s.startsWith('"""', i)) {
        inPara = false;
        i += 3;
        continue;
      }
      i++;
      continue;
    }
    if (s.startsWith('"""', i)) {
      inPara = true;
      i += 3;
      continue;
    }
    const c = s[i];
    if (c === '{') {
      depth++;
    } else if (c === '}') {
      depth--;
    }
    i++;
  }
  return { depth, inPara };
}

/**
 * Re-indents a nassi document based on brace nesting.
 *
 * - Lines that live inside a `"""` paragraph are left completely untouched,
 *   because paragraph content is literal markdown where leading/trailing
 *   whitespace can be significant.
 * - Metadata lines (starting with `;`) are kept verbatim (trailing whitespace
 *   trimmed) so the author's column alignment is preserved.
 * - Every other line is trimmed and re-indented to `depth * indentUnit`, where
 *   a line that begins with `}` is dedented by one level.
 *
 * @param {string} text
 * @param {string} indentUnit
 * @returns {string}
 */
function formatNassi(text, indentUnit) {
  const eol = text.indexOf('\r\n') !== -1 ? '\r\n' : '\n';
  const endsWithNewline = /\r?\n$/.test(text);
  const lines = text.split(/\r?\n/);
  if (endsWithNewline) {
    lines.pop(); // drop the empty element produced by a trailing newline
  }

  const out = [];
  let depth = 0;
  let inPara = false;

  for (const raw of lines) {
    if (inPara) {
      // Inside a paragraph: keep the line exactly as-is, just track the state.
      out.push(raw);
      ({ depth, inPara } = scanLine(raw, depth, inPara));
      continue;
    }

    const trimmed = raw.trim();

    if (trimmed === '') {
      out.push('');
      continue;
    }

    // Preserve metadata lines verbatim (sans trailing whitespace). Their
    // values are arbitrary single-line text, so braces and `"""` in them are
    // never structural — don't scan them.
    if (trimmed.startsWith(';')) {
      out.push(raw.replace(/[ \t]+$/, ''));
      continue;
    }

    const startsWithClose = trimmed[0] === '}';
    const indentDepth = Math.max(depth - (startsWithClose ? 1 : 0), 0);
    out.push(indentUnit.repeat(indentDepth) + trimmed);

    const next = scanLine(trimmed, depth, inPara);
    depth = Math.max(next.depth, 0);
    inPara = next.inPara;
  }

  return out.join(eol) + (endsWithNewline ? eol : '');
}

/**
 * Builds an INCLUDE resolver for lintNassi that reads files relative to
 * `dir`. `visited` holds the absolute paths on the current include chain, so
 * cyclic includes are skipped instead of recursing forever.
 *
 * @param {string} dir
 * @param {Set<string>} visited
 */
function makeIncludeResolver(dir, visited) {
  return (filename) => {
    const file = path.resolve(dir, filename);
    if (visited.has(file)) {
      return { text: '' }; // cyclic include: nothing further to analyze
    }
    let text;
    try {
      text = fs.readFileSync(file, 'utf8');
    } catch (e) {
      return null;
    }
    const chain = new Set(visited);
    chain.add(file);
    return { text, resolveInclude: makeIncludeResolver(path.dirname(file), chain) };
  };
}

const SEVERITIES = {
  error: vscode.DiagnosticSeverity.Error,
  warning: vscode.DiagnosticSeverity.Warning,
  info: vscode.DiagnosticSeverity.Information
};

function refreshDiagnostics(document, collection) {
  if (document.languageId !== 'nassi') {
    return;
  }
  const opts = {};
  if (document.uri.scheme === 'file') {
    opts.resolveInclude = makeIncludeResolver(
      path.dirname(document.uri.fsPath),
      new Set([document.uri.fsPath])
    );
  }
  const diagnostics = lintNassi(document.getText(), opts).map((d) => {
    const range = new vscode.Range(d.line, d.startCol, d.line, d.endCol);
    const diagnostic = new vscode.Diagnostic(range, d.message, SEVERITIES[d.severity]);
    diagnostic.source = 'nassi';
    diagnostic.code = d.code;
    return diagnostic;
  });
  collection.set(document.uri, diagnostics);
}

function activate(context) {
  const selector = { language: 'nassi' };

  const provider = {
    provideDocumentFormattingEdits(document, options) {
      const indentUnit = options.insertSpaces
        ? ' '.repeat(options.tabSize)
        : '\t';
      const formatted = formatNassi(document.getText(), indentUnit);
      const fullRange = new vscode.Range(
        document.positionAt(0),
        document.positionAt(document.getText().length)
      );
      return [vscode.TextEdit.replace(fullRange, formatted)];
    }
  };

  context.subscriptions.push(
    vscode.languages.registerDocumentFormattingEditProvider(selector, provider)
  );

  const diagnostics = vscode.languages.createDiagnosticCollection('nassi');
  const lintTimers = new Map();

  const scheduleLint = (document) => {
    if (document.languageId !== 'nassi') {
      return;
    }
    const key = document.uri.toString();
    clearTimeout(lintTimers.get(key));
    lintTimers.set(key, setTimeout(() => {
      lintTimers.delete(key);
      refreshDiagnostics(document, diagnostics);
    }, 300));
  };

  context.subscriptions.push(
    diagnostics,
    { dispose() { lintTimers.forEach(clearTimeout); lintTimers.clear(); } },
    vscode.workspace.onDidOpenTextDocument((d) => refreshDiagnostics(d, diagnostics)),
    vscode.workspace.onDidChangeTextDocument((e) => scheduleLint(e.document)),
    vscode.workspace.onDidCloseTextDocument((d) => {
      const key = d.uri.toString();
      clearTimeout(lintTimers.get(key));
      lintTimers.delete(key);
      diagnostics.delete(d.uri);
    })
  );

  vscode.workspace.textDocuments.forEach((d) => refreshDiagnostics(d, diagnostics));
}

function deactivate() {}

module.exports = { activate, deactivate, formatNassi };
