# nassi — VS Code extension

Syntax highlighting and formatting for [nassi](https://github.com/phoenixreisen/nassi)
specification files (`.nassi`).

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

## Try it out

Open this folder in VS Code and press <kbd>F5</kbd> to launch an Extension
Development Host, then open one of the files in `../../examples`.

## Install locally

```sh
cd extra/vscode/dist
code --install-extension nassi-0.1.0.vsix
```

## Create extension locally

```sh
npm install -g @vscode/vsce
cd extra/vscode
vsce package
```

There is no build step — the extension is plain JavaScript.
