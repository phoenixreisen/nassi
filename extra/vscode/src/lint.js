'use strict';

/**
 * A lightweight linter for nassi documents, mirroring resources/grammar.bnf.
 *
 * The grammar is very forgiving: almost any line that fails to parse as a
 * keyword statement degrades to a plain TEXT statement. The only hard parse
 * errors in real nassi are structural: stray braces, a `{` that does not
 * follow a block keyword, malformed CATCH/SWITCH bodies, and unterminated
 * `"""` paragraphs. This linter reports exactly those as errors, mirrors the
 * CLI's exception warnings (src/nassi/warn.clj), and adds informational hints
 * when an ALL-CAPS keyword line silently degrades to plain text.
 *
 * Diagnostics are plain objects so this module stays independent of the
 * vscode API (and therefore testable with `node --test`):
 *
 *   { severity: 'error'|'warning'|'info', code, message,
 *     line, startCol, endCol }   // all zero-based, single-line ranges
 */

const BLOCK_KEYWORDS = new Set(['for', 'while', 'until', 'sub', 'if']);
const KEYWORDS = new Set([
  'preamble', 'epilogue', 'include', 'throw', 'catch', 'handle',
  'switch', 'case', 'default', 'else', ...BLOCK_KEYWORDS
]);

const WORD_RE = /^[A-Za-z][A-Za-z0-9_.-]*/;
const ERRORCODE_RE = /^#[A-Za-z][.A-Za-z0-9_-]+/;
const FILENAME_RE = /^"[^"\n]*"/;
const SENTENCE_RE = /^[^\n{}]+/;
const METAINFO_RE = /^;[ \t]*[A-Za-z][.A-Za-z0-9_-]+[ \t]*=[^\n]*(\r\n|\n|$)/;
const METAINFO_LIKE_RE = /^;[ \t]*[A-Za-z][.A-Za-z0-9_-]+[ \t]*=/;

class Scanner {
  constructor(text) {
    this.text = text;
    this.pos = 0;
    this.line = 0;
    this.col = 0;
  }

  eof() { return this.pos >= this.text.length; }

  peek() { return this.text[this.pos]; }

  startsWith(s) { return this.text.startsWith(s, this.pos); }

  save() { return { pos: this.pos, line: this.line, col: this.col }; }

  restore(state) {
    this.pos = state.pos;
    this.line = state.line;
    this.col = state.col;
  }

  advance(n) {
    for (let i = 0; i < n && this.pos < this.text.length; i++) {
      if (this.text[this.pos] === '\n') {
        this.line++;
        this.col = 0;
      } else {
        this.col++;
      }
      this.pos++;
    }
  }

  skipWs() {
    while (!this.eof() && /\s/.test(this.peek())) this.advance(1);
  }

  /** Matches `re` (which must be anchored with `^`) without consuming. */
  lookRe(re) {
    const m = re.exec(this.text.slice(this.pos));
    return m ? m[0] : null;
  }

  /** Matches `re` and consumes it. Returns the match plus its range. */
  takeRe(re) {
    const m = this.lookRe(re);
    if (m === null) return null;
    const token = { value: m, line: this.line, startCol: this.col };
    this.advance(m.length);
    token.endCol = token.line === this.line ? this.col : this.col + m.length;
    return token;
  }
}

function makeCtx(resolveInclude) {
  return { diags: [], thrown: [], handled: [], resolveInclude };
}

function diag(ctx, severity, code, message, line, startCol, endCol) {
  ctx.diags.push({ severity, code, message, line, startCol, endCol });
}

/** Turns an error-code token from Scanner#takeRe into a thrown/handled record. */
function errorCode(token) {
  return {
    code: token.value,
    line: token.line,
    startCol: token.startCol,
    endCol: token.endCol
  };
}

/**
 * Consumes a `"""..."""` paragraph. Reports an error if it never closes.
 */
function consumeParagraph(s, ctx) {
  const start = s.save();
  const end = s.text.indexOf('"""', s.pos + 3);
  if (end === -1) {
    diag(ctx, 'error', 'unterminated-paragraph',
      'Unterminated paragraph: missing closing \'"""\'.',
      start.line, start.col, start.col + 3);
    s.advance(s.text.length - s.pos);
    return;
  }
  s.advance(end + 3 - s.pos);
}

/**
 * TEXT = PARAGRAPH / SENTENCE (whitespace before it, including newlines, is
 * insignificant). Returns true if some text was consumed.
 */
function tryText(s, ctx) {
  s.skipWs();
  if (s.startsWith('"""')) {
    consumeParagraph(s, ctx);
    return true;
  }
  return s.takeRe(SENTENCE_RE) !== null;
}

/**
 * Consumes the current statement as a plain TEXT statement (the grammar's
 * fallback for everything that is not a keyword construct).
 */
function plainText(s, ctx) {
  if (s.startsWith('"""')) {
    consumeParagraph(s, ctx);
    return;
  }
  const sentence = s.takeRe(SENTENCE_RE);
  if (sentence === null) {
    s.advance(1); // defensive: never get stuck
    return;
  }
  if (METAINFO_LIKE_RE.test(sentence.value)) {
    diag(ctx, 'info', 'misplaced-metadata',
      'This looks like a metadata entry, but metadata is only recognized at the very top of the file. The line is treated as diagram text.',
      sentence.line, sentence.startCol, sentence.endCol);
  }
}

/**
 * Emits a hint when an ALL-CAPS keyword degrades to plain text, since that is
 * almost always a typo rather than intentional prose.
 */
function capsHint(ctx, word, detail) {
  if (word.value.length > 1 && word.value === word.value.toUpperCase()) {
    diag(ctx, 'info', 'keyword-as-text',
      `'${word.value}' ${detail}, so this line is treated as plain text.`,
      word.line, word.startCol, word.endCol);
  }
}

/** BLOCK = '{' STATEMENT+ '}' — caller must have peeked the `{`. */
function parseBlock(s, ctx) {
  const brace = s.save();
  s.advance(1);
  const count = parseStatements(s, ctx, brace);
  if (count === 0) {
    diag(ctx, 'error', 'empty-block',
      'A block must contain at least one statement.',
      brace.line, brace.col, brace.col + 1);
  }
}

/** CATCH body: '{' HANDLE+ '}' — caller must have peeked the `{`. */
function parseCatchBody(s, ctx) {
  const brace = s.save();
  s.advance(1);
  let handles = 0;
  for (;;) {
    s.skipWs();
    if (s.eof()) {
      diag(ctx, 'error', 'unclosed-brace',
        "Unclosed '{': missing matching '}'.", brace.line, brace.col, brace.col + 1);
      break;
    }
    if (s.peek() === '}') {
      s.advance(1);
      break;
    }
    const word = s.takeRe(WORD_RE);
    if (word !== null && word.value.toLowerCase() === 'handle') {
      handles++;
      let codes = 0;
      for (;;) {
        s.skipWs();
        const code = s.takeRe(ERRORCODE_RE);
        if (code === null) break;
        codes++;
        ctx.handled.push(errorCode(code));
      }
      if (codes === 0) {
        diag(ctx, 'error', 'handle-without-code',
          "Expected one or more error-codes (e.g. '#my-error') after HANDLE.",
          word.line, word.startCol, word.endCol);
      }
      s.skipWs();
      if (s.peek() === '{') {
        parseBlock(s, ctx);
      } else {
        diag(ctx, 'error', 'handle-without-block',
          "Expected a '{ ... }' block after HANDLE.",
          word.line, word.startCol, word.endCol);
      }
    } else {
      const at = word !== null ? word : { line: s.line, startCol: s.col, endCol: s.col + 1 };
      diag(ctx, 'error', 'expected-handle',
        "Only HANDLE statements are allowed inside CATCH { ... }.",
        at.line, at.startCol, at.endCol);
      // Recover: skip the offending statement, keeping braces balanced.
      if (word === null) parseStatement(s, ctx); else plainText(s, ctx);
    }
  }
  if (handles === 0) {
    diag(ctx, 'error', 'empty-catch',
      'CATCH must contain at least one HANDLE.',
      brace.line, brace.col, brace.col + 1);
  }
}

/** SWITCH body: '{' (CASE | DEFAULT)+ '}' — caller must have peeked the `{`. */
function parseSwitchBody(s, ctx) {
  const brace = s.save();
  s.advance(1);
  let cases = 0;
  for (;;) {
    s.skipWs();
    if (s.eof()) {
      diag(ctx, 'error', 'unclosed-brace',
        "Unclosed '{': missing matching '}'.", brace.line, brace.col, brace.col + 1);
      break;
    }
    if (s.peek() === '}') {
      s.advance(1);
      break;
    }
    const word = s.takeRe(WORD_RE);
    const kw = word !== null ? word.value.toLowerCase() : null;
    if (kw === 'case' || kw === 'default') {
      cases++;
      const gotText = tryText(s, ctx);
      if (!gotText) {
        diag(ctx, 'error', 'case-without-text',
          `Expected a text after ${kw.toUpperCase()}.`,
          word.line, word.startCol, word.endCol);
      }
      s.skipWs();
      if (s.peek() === '{') {
        parseBlock(s, ctx);
      } else {
        diag(ctx, 'error', 'case-without-block',
          `Expected a '{ ... }' block after ${kw.toUpperCase()}.`,
          word.line, word.startCol, word.endCol);
      }
    } else {
      const at = word !== null ? word : { line: s.line, startCol: s.col, endCol: s.col + 1 };
      diag(ctx, 'error', 'expected-case',
        "Only CASE and DEFAULT statements are allowed inside SWITCH { ... }.",
        at.line, at.startCol, at.endCol);
      if (word === null) parseStatement(s, ctx); else plainText(s, ctx);
    }
  }
  if (cases === 0) {
    diag(ctx, 'error', 'empty-switch',
      'SWITCH must contain at least one CASE or DEFAULT.',
      brace.line, brace.col, brace.col + 1);
  }
}

function parseInclude(s, ctx, word, start) {
  s.skipWs();
  const fname = s.takeRe(FILENAME_RE);
  if (fname === null) {
    s.restore(start);
    capsHint(ctx, word, 'is not followed by a quoted filename');
    plainText(s, ctx);
    return;
  }
  if (ctx.resolveInclude === undefined) return;
  const name = fname.value.slice(1, -1);
  const res = ctx.resolveInclude(name);
  if (res === null) {
    diag(ctx, 'warning', 'include-not-found',
      `Included file ${fname.value} not found.`,
      fname.line, fname.startCol, fname.endCol);
    return;
  }
  if (!res.text) return; // empty or cyclic include: nothing to analyze
  // Collect THROW/HANDLE error-codes from the included diagram (the CLI
  // embeds included diagrams before its exception analysis). Diagnostics of
  // the included file itself are not reported here.
  const sub = makeCtx(res.resolveInclude);
  parseSource(res.text, sub);
  for (const t of sub.thrown) {
    ctx.thrown.push({ ...errorCode(fname), code: t.code, via: name });
  }
  for (const h of sub.handled) {
    ctx.handled.push({ ...errorCode(fname), code: h.code, via: name });
  }
}

function parseStatement(s, ctx) {
  if (s.startsWith('"""')) {
    consumeParagraph(s, ctx);
    return;
  }
  if (s.peek() === '"') { // lone quote: cannot start a keyword, plain text
    plainText(s, ctx);
    return;
  }
  const start = s.save();
  const word = s.takeRe(WORD_RE);
  const kw = word !== null && KEYWORDS.has(word.value.toLowerCase())
    ? word.value.toLowerCase()
    : null;
  if (kw === null) {
    s.restore(start);
    plainText(s, ctx);
    return;
  }

  if (kw === 'preamble' || kw === 'epilogue') {
    if (!tryText(s, ctx)) {
      s.restore(start);
      capsHint(ctx, word, 'is not followed by a text');
      plainText(s, ctx);
    }
    return;
  }

  if (kw === 'include') {
    parseInclude(s, ctx, word, start);
    return;
  }

  if (kw === 'throw') {
    s.skipWs();
    const code = s.takeRe(ERRORCODE_RE);
    if (code !== null && tryText(s, ctx)) {
      ctx.thrown.push(errorCode(code));
    } else {
      s.restore(start);
      capsHint(ctx, word,
        "is not followed by an error-code (e.g. '#my-error') and a text");
      plainText(s, ctx);
    }
    return;
  }

  if (BLOCK_KEYWORDS.has(kw)) {
    const gotText = tryText(s, ctx);
    s.skipWs();
    if (s.peek() !== '{') {
      s.restore(start);
      capsHint(ctx, word, "is not followed by a '{ ... }' block");
      plainText(s, ctx);
      return;
    }
    if (!gotText) {
      diag(ctx, 'error', 'missing-text',
        `Expected a text between ${kw.toUpperCase()} and '{'.`,
        word.line, word.startCol, word.endCol);
    }
    parseBlock(s, ctx);
    if (kw === 'if') {
      const beforeElse = s.save();
      s.skipWs();
      const next = s.takeRe(WORD_RE);
      if (next !== null && next.value.toLowerCase() === 'else') {
        s.skipWs();
        if (s.peek() === '{') {
          parseBlock(s, ctx);
          return;
        }
      }
      s.restore(beforeElse);
    }
    return;
  }

  if (kw === 'switch') {
    const gotText = tryText(s, ctx);
    s.skipWs();
    if (s.peek() !== '{') {
      s.restore(start);
      capsHint(ctx, word, "is not followed by a '{ ... }' block");
      plainText(s, ctx);
      return;
    }
    if (!gotText) {
      diag(ctx, 'error', 'missing-text',
        "Expected a text between SWITCH and '{'.",
        word.line, word.startCol, word.endCol);
    }
    parseSwitchBody(s, ctx);
    return;
  }

  if (kw === 'catch') {
    s.skipWs();
    if (s.peek() !== '{') {
      s.restore(start);
      capsHint(ctx, word, "is not followed by a '{ ... }' block");
      plainText(s, ctx);
      return;
    }
    parseCatchBody(s, ctx);
    return;
  }

  if (kw === 'else') {
    s.skipWs();
    if (s.peek() === '{') {
      diag(ctx, 'error', 'else-without-if',
        'ELSE without a matching IF: ELSE must directly follow the closing brace of an IF block.',
        word.line, word.startCol, word.endCol);
      parseBlock(s, ctx);
      return;
    }
    s.restore(start);
    capsHint(ctx, word, 'is only recognized directly after an IF block');
    plainText(s, ctx);
    return;
  }

  if (kw === 'handle') {
    s.skipWs();
    if (s.peek() === '{' || s.lookRe(ERRORCODE_RE) !== null) {
      diag(ctx, 'error', 'handle-outside-catch',
        'HANDLE is only allowed inside a CATCH { ... } block.',
        word.line, word.startCol, word.endCol);
      for (;;) { // recover: skip codes and block
        s.skipWs();
        if (s.takeRe(ERRORCODE_RE) === null) break;
      }
      s.skipWs();
      if (s.peek() === '{') parseBlock(s, ctx);
      return;
    }
    s.restore(start);
    plainText(s, ctx);
    return;
  }

  // case | default outside of SWITCH
  tryText(s, ctx);
  s.skipWs();
  if (s.peek() === '{') {
    diag(ctx, 'error', 'case-outside-switch',
      `${kw.toUpperCase()} is only allowed inside a SWITCH { ... } block.`,
      word.line, word.startCol, word.endCol);
    parseBlock(s, ctx);
    return;
  }
  s.restore(start);
  plainText(s, ctx);
}

/**
 * Parses statements until EOF (openBrace = null) or until the `}` matching
 * `openBrace` is consumed. Returns the number of statements parsed.
 */
function parseStatements(s, ctx, openBrace) {
  let count = 0;
  for (;;) {
    s.skipWs();
    if (s.eof()) {
      if (openBrace !== null) {
        diag(ctx, 'error', 'unclosed-brace',
          "Unclosed '{': missing matching '}'.",
          openBrace.line, openBrace.col, openBrace.col + 1);
      }
      return count;
    }
    const c = s.peek();
    if (c === '}') {
      if (openBrace !== null) {
        s.advance(1);
        return count;
      }
      diag(ctx, 'error', 'unmatched-brace',
        "Unexpected '}': no matching '{'.", s.line, s.col, s.col + 1);
      s.advance(1);
      continue;
    }
    if (c === '{') {
      diag(ctx, 'error', 'unexpected-brace',
        "Unexpected '{': a block can only follow FOR, WHILE, UNTIL, SUB, IF, ELSE, SWITCH, CASE, DEFAULT, CATCH or HANDLE.",
        s.line, s.col, s.col + 1);
      const brace = s.save();
      s.advance(1);
      parseStatements(s, ctx, brace); // recover, keeping braces balanced
      continue;
    }
    parseStatement(s, ctx);
    count++;
  }
}

function parseSource(text, ctx) {
  const s = new Scanner(text);
  // METADATA is only recognized at the very start of the file (no leading
  // whitespace), one `; key = value` line after another.
  while (s.lookRe(METAINFO_RE) !== null) {
    s.advance(s.lookRe(METAINFO_RE).length);
  }
  parseStatements(s, ctx, null);
}

/**
 * Mirrors nassi.warn/exception-warnings: warns about thrown error-codes
 * without a handler, and about handlers for codes that are never thrown.
 */
function exceptionWarnings(ctx) {
  const thrownCodes = new Set(ctx.thrown.map((t) => t.code));
  const handledCodes = new Set(ctx.handled.map((h) => h.code));
  for (const t of ctx.thrown) {
    if (!handledCodes.has(t.code)) {
      const via = t.via ? ` (thrown in included file "${t.via}")` : '';
      diag(ctx, 'warning', 'unhandled-error-code',
        `No handler defined for error-code '${t.code}'${via}.`,
        t.line, t.startCol, t.endCol);
    }
  }
  for (const h of ctx.handled) {
    if (!thrownCodes.has(h.code)) {
      const via = h.via ? ` (handled in included file "${h.via}")` : '';
      diag(ctx, 'warning', 'needless-handler',
        `Error-code '${h.code}' is handled${via}, but there is no THROW statement with that error-code.`,
        h.line, h.startCol, h.endCol);
    }
  }
}

/**
 * Lints a nassi document.
 *
 * @param {string} text the document content
 * @param {object} [opts]
 * @param {(filename: string) => null | { text: string, resolveInclude?: Function }} [opts.resolveInclude]
 *   Resolves an INCLUDE filename to its content (plus a resolver for nested
 *   includes, relative to that file). Return `null` if the file does not
 *   exist, or `{ text: '' }` to skip it (e.g. cyclic includes). When omitted,
 *   INCLUDE statements are not checked or expanded.
 * @returns {Array<{severity: string, code: string, message: string,
 *                  line: number, startCol: number, endCol: number}>}
 */
function lintNassi(text, opts = {}) {
  const ctx = makeCtx(opts.resolveInclude);
  parseSource(text, ctx);
  exceptionWarnings(ctx);
  return ctx.diags.sort((a, b) => a.line - b.line || a.startCol - b.startCol);
}

module.exports = { lintNassi };
