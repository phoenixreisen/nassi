# nassi — VS Code extension

Syntax highlighting, formatting and linting for
[nassi](https://github.com/phoenixreisen/nassi) specification files (`.nassi`).

## Features

- **Syntax highlighting** (TextMate grammar) for all nassi constructs:
  - keywords: `PREAMBLE`, `EPILOGUE`, `INCLUDE`, `FOR`, `WHILE`, `UNTIL`, `SUB`,
    `IF`, `ELSE`, `SWITCH`, `CASE`, `DEFAULT`, `THROW`, `CATCH`, `HANDLE`
  - error codes (`#wrong-password`) and anchors (`!!my-id`)
  - metadata key/value lines (`; Author = ...`)
  - `"""…"""` paragraphs and `INCLUDE "…"` filenames
  - inline Markdown inside steps (headings, bold, italic/`<i>`, links, `code`)
- **Formatting** (`Format Document`, <kbd>⇧⌥F</kbd>): re-indents blocks based on
  `{` / `}` nesting using your editor's tab settings. Paragraph (`"""…"""`)
  interiors are left untouched, since their content is literal Markdown where
  whitespace can be significant. Metadata lines are kept verbatim so column
  alignment is preserved.
- **Linting** (diagnostics in the editor and the *Problems* panel, updated as
  you type):
  - *Errors* — structural problems that make the file unparseable for the
    nassi CLI: unbalanced `{` / `}`, a `{` that does not follow a block
    keyword, empty blocks, unterminated `"""` paragraphs, `CATCH` without
    `HANDLE`, `SWITCH` without `CASE`/`DEFAULT`, `ELSE` without a matching
    `IF`, and `HANDLE`/`CASE`/`DEFAULT` outside their container.
  - *Warnings* — the same exception checks the nassi CLI performs: `THROW`n
    error-codes that no `HANDLE` covers, and handled error-codes that are
    never thrown. `INCLUDE`d files are taken into account (and a warning is
    shown if an included file cannot be found).
  - *Hints* — an ALL-CAPS keyword line that silently degrades to plain text
    (e.g. `SUB foo` without a `{ ... }` block, or `THROW` without an
    error-code), and metadata-style lines (`; key = value`) that appear below
    the top of the file.

The linter (`src/lint.js`) has no dependencies; its tests run with `npm test`
(Node ≥ 18).

## Try it out

Open this folder in VS Code and press <kbd>F5</kbd> to launch an Extension
Development Host, then open one of the files in `../../examples`.

## Install locally

```sh
cd extra/vscode/dist
code --install-extension nassi-0.2.0.vsix
```

## Create extension locally

```sh
npm install -g @vscode/vsce
cd extra/vscode
vsce package
```

There is no build step — the extension is plain JavaScript.
