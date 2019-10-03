package com.apurebase.kgraphql.request

import com.apurebase.kgraphql.RequestException

fun validateAndFilterRequest(request : String) : String{
    return request
            .validateCharacters()
            .dropUnicodeBOM()
            .filterComments()
}

private fun String.dropUnicodeBOM() : String {
    return if(startsWith('\uFEFF')){
        this.drop(1)
    } else {
        this
    }
}

private fun String.filterComments() = replace(Regex("#.*\n"), "")

//validate characters, will be moved because according to spec restrictions are only on documents tokens
private fun String.validateCharacters(): String {
    forEach { char ->
        when (char) {
            '\u0009', '\u000A', '\u000D', in '\u0020'..'\uFFFF' -> {
            }
            else -> throw RequestException("Illegal character: ${String.format("\\u%04x", char.toInt())}")
        }
    }
    return this
}
