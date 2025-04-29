" Vim syntax file
" Language:	    textual representations of nassi-shneiderman diagrams
" URI:          https://github.com/phoenixreisen/nassi
" Maintainer:	Phoenix Reisen GmbH
" Last Change:	2024 Oct 08
"
" Add this to your .vimrc file:
" autocmd BufRead,BufNewFile *.nassi set filetype=nassi

if exists("b:current_syntax")
    finish
endif

syntax clear
syntax case ignore
syntax keyword nassiKeyword preamble epilogue
syntax keyword nassiKeyword if else
syntax keyword nassiKeyword switch case default
syntax keyword nassiKeyword for while until sub
syntax keyword nassiKeyword throw catch handle
syntax keyword nassiDirective include
syntax match paragraphDelim '\"\"\"'
syntax match beginBlock '{'
syntax match endBlock '}'
syntax match anchor '!![A-Za-z][\.A-Za-z0-9_-]*'

highlight def link nassiKeyword Keyword
highlight def link nassiDirective Include
highlight def link paragraphDelim Special
highlight def link beginBlock Special
highlight def link endBlock Special
highlight def link anchor Constant

let b:current_syntax = 'nassi'
