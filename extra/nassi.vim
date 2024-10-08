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
syntax keyword usecaseKeyword if else
syntax keyword usecaseKeyword switch case default
syntax keyword usecaseKeyword for while until
syntax keyword usecaseKeyword throw catch handle
syntax match paragraphDelim '\"\"\"'
syntax match beginBlock '{'
syntax match endBlock '}'
syntax match anchor '!![A-Za-z][\.A-Za-z0-9_-]*'

highlight def link usecaseKeyword Keyword
highlight def link paragraphDelim Special
highlight def link beginBlock Special
highlight def link endBlock Special
highlight def link anchor Constant

let b:current_syntax = 'nassi'
