# nassi — Notepad++ support

A **User Defined Language** (UDL) for syntax highlighting (no plugin, no compilation) in Notepadd++.

## 1. Syntax highlighting — `nassi.udl.xml`

1. **Language → User Defined Language → Define your language…**
2. **Import…**, select `nassi.udl.xml`.
3. Restart Notepad++.

`.nassi` files are then detected automatically. Highlights keywords, `{ }`
blocks (with folding), `"""…"""` paragraphs and `"…"` strings, `!!anchors`,
`#error-codes`, and `; key = value` metadata lines.

UDL has no regular expressions, so a few things are approximate — see the
comment block at the top of `nassi.udl.xml` for the exact caveats.
