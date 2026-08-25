'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
const { lintNassi } = require('../src/lint.js');

function codes(diags) {
  return diags.map((d) => d.code);
}

test('clean diagram produces no diagnostics', () => {
  const text = [
    '; Author = Phoenix Reisen',
    'PREAMBLE """Some *markdown* preamble."""',
    '',
    '1 Do something',
    'SUB 2 Steps {',
    '  THROW #foo 2.1 Something went wrong',
    '}',
    'SUB 3 Cleanup {',
    '  CATCH {',
    '    HANDLE #foo { 2.1.1 Recover }',
    '  }',
    '}',
    'IF something {',
    '  4 then-branch',
    '} ELSE {',
    '  5 else-branch',
    '}',
    'SWITCH color {',
    '  CASE red   { 6 stop }',
    '  DEFAULT go { 7 go }',
    '}'
  ].join('\n');
  assert.deepEqual(lintNassi(text), []);
});

test('keywords are case-insensitive', () => {
  const text = 'sub 1 steps {\n  throw #xx 1.1 oops\n  catch {\n    handle #xx { 1.1.1 fix }\n  }\n}';
  assert.deepEqual(lintNassi(text), []);
});

test('unclosed brace is an error at the opening brace', () => {
  const diags = lintNassi('SUB 1 text {\n  1.1 text\n');
  assert.deepEqual(codes(diags), ['unclosed-brace']);
  assert.equal(diags[0].severity, 'error');
  assert.equal(diags[0].line, 0);
  assert.equal(diags[0].startCol, 11);
});

test('unmatched closing brace is an error', () => {
  const diags = lintNassi('1 text\n}\n');
  assert.deepEqual(codes(diags), ['unmatched-brace']);
  assert.equal(diags[0].line, 1);
});

test('brace without block keyword is an error', () => {
  const diags = lintNassi('just some text\n{\n  1 text\n}\n');
  assert.deepEqual(codes(diags), ['unexpected-brace']);
});

test('unterminated paragraph is an error', () => {
  const diags = lintNassi('1 text\n"""never closed\n');
  assert.deepEqual(codes(diags), ['unterminated-paragraph']);
  assert.equal(diags[0].line, 1);
});

test('empty block is an error', () => {
  const diags = lintNassi('SUB 1 text { }\n');
  assert.deepEqual(codes(diags), ['empty-block']);
});

test('missing text before block is an error', () => {
  const diags = lintNassi('SUB {\n  1 text\n}\n');
  assert.deepEqual(codes(diags), ['missing-text']);
});

test('CATCH without HANDLE is an error', () => {
  const diags = lintNassi('CATCH {\n  1 text\n}\n');
  assert.deepEqual(codes(diags), ['empty-catch', 'expected-handle']);
});

test('SWITCH without CASE/DEFAULT is an error', () => {
  const diags = lintNassi('SWITCH x {\n  1 text\n}\n');
  assert.deepEqual(codes(diags), ['empty-switch', 'expected-case']);
});

test('ELSE without IF is an error', () => {
  const diags = lintNassi('ELSE {\n  1 text\n}\n');
  assert.deepEqual(codes(diags), ['else-without-if']);
});

test('HANDLE outside CATCH is an error', () => {
  const diags = lintNassi('HANDLE #foo {\n  1 text\n}\n');
  assert.deepEqual(codes(diags), ['handle-outside-catch']);
});

test('CASE outside SWITCH is an error', () => {
  const diags = lintNassi('CASE red {\n  1 text\n}\n');
  assert.deepEqual(codes(diags), ['case-outside-switch']);
});

test('thrown error-code without handler is a warning', () => {
  const diags = lintNassi('THROW #boom 1 text\n');
  assert.deepEqual(codes(diags), ['unhandled-error-code']);
  assert.equal(diags[0].severity, 'warning');
  assert.match(diags[0].message, /#boom/);
  assert.equal(diags[0].startCol, 6);
});

test('handled error-code that is never thrown is a warning', () => {
  const diags = lintNassi('CATCH {\n  HANDLE #ghost { 1 text }\n}\n');
  assert.deepEqual(codes(diags), ['needless-handler']);
  assert.equal(diags[0].severity, 'warning');
  assert.match(diags[0].message, /#ghost/);
});

test('HANDLE may handle multiple error-codes', () => {
  const text = 'THROW #aa 1 x\nTHROW #bb 2 y\nCATCH {\n  HANDLE #aa #bb { 3 z }\n}\n';
  assert.deepEqual(lintNassi(text), []);
});

test('ALL-CAPS keyword degrading to text yields an info hint', () => {
  const diags = lintNassi('SUB without a block\n');
  assert.deepEqual(codes(diags), ['keyword-as-text']);
  assert.equal(diags[0].severity, 'info');
});

test('lowercase keyword-like prose yields no hint', () => {
  assert.deepEqual(lintNassi('for example, this is fine\n'), []);
  assert.deepEqual(lintNassi('if you want to know more, read on\n'), []);
});

test('THROW without error-code yields an info hint', () => {
  const diags = lintNassi('THROW something without a code\n');
  assert.deepEqual(codes(diags), ['keyword-as-text']);
});

test('metadata at top of file is accepted, later metadata yields info', () => {
  const ok = lintNassi('; Author = me\n; Version = 1\n1 text\n');
  assert.deepEqual(ok, []);
  const diags = lintNassi('1 text\n; Author = me\n');
  assert.deepEqual(codes(diags), ['misplaced-metadata']);
  assert.equal(diags[0].severity, 'info');
});

test('missing include file is a warning', () => {
  const diags = lintNassi('INCLUDE "missing.nassi"\n1 text\n', {
    resolveInclude: () => null
  });
  assert.deepEqual(codes(diags), ['include-not-found']);
  assert.equal(diags[0].severity, 'warning');
});

test('include statements are skipped when no resolver is given', () => {
  assert.deepEqual(lintNassi('INCLUDE "whatever.nassi"\n1 text\n'), []);
});

test('error-codes thrown in included files count as thrown', () => {
  const text = 'INCLUDE "sub.nassi"\nCATCH {\n  HANDLE #foo { 1 text }\n}\n';
  const diags = lintNassi(text, {
    resolveInclude: () => ({ text: 'THROW #foo 1 text\n' })
  });
  assert.deepEqual(diags, []);
});

test('unhandled error-code from an included file warns at the INCLUDE', () => {
  const diags = lintNassi('INCLUDE "sub.nassi"\n1 text\n', {
    resolveInclude: () => ({ text: 'THROW #boom 1 text\n' })
  });
  assert.deepEqual(codes(diags), ['unhandled-error-code']);
  assert.equal(diags[0].line, 0);
  assert.match(diags[0].message, /sub\.nassi/);
});

test('paragraphs may contain braces without affecting nesting', () => {
  const text = 'SUB 1 text {\n  """a paragraph with { and } inside"""\n}\n';
  assert.deepEqual(lintNassi(text), []);
});

test('text and block may be spread over several lines', () => {
  const text = 'SUB\n1 text\n{\n  1.1 text\n}\n';
  assert.deepEqual(lintNassi(text), []);
});

test('IF with paragraph condition', () => {
  const text = 'IF """is it\nraining?""" {\n  1 text\n}\n';
  assert.deepEqual(lintNassi(text), []);
});
