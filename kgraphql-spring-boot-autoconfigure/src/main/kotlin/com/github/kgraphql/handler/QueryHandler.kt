package com.github.kgraphql.handler

const val QUERY = "\"query\":\""
const val VARIABLES = "\"variables\":"
const val DOUBLE_QUOTE = '"'
const val BACK_SLASH = '\\'
const val N = 'n'
const val T = 't'
const val R = 'r'
const val CURLY_BRACKETS_OPEN = "{"
const val CURLY_BRACKETS_CLOSE = '}'
const val HASH_SYMBOL = '#'
const val NULL = "null"

var variables: String? = null
var query = ""

fun handle(request: String)  {
    query = ""
    variables = null
    var queryWithoutComment = request.removeAllComments2()
    queryWithoutComment = queryWithoutComment.removeAllComments()
    queryWithoutComment.toKgraphqlVariables()
    queryWithoutComment.toKgraphqlQuery()
}

private fun String.removeAllComments(): String {
    var result = ""
    var i = 0
    this.forEach { _ ->
        if (this[i] != '\n' && this[i] != '\t' && this[i] != '\r') {
            result += this[i]
        }
        i++
    }
    return result
}

private fun String.removeAllComments2(): String {
    var result = ""
    var i = 0
    var slashCaraFound = false
    var isHashSymbolFound  = false
    this.forEach { _ ->
        if (!isHashSymbolFound || (this[i] == BACK_SLASH && (this[i+1] == N || this[i+1] == T || this[i+1] == R))) {
            isHashSymbolFound = false
            if (this[i] == HASH_SYMBOL) {
                isHashSymbolFound = true
            } else {
                if (slashCaraFound && (this[i] == N || this[i] == T || this[i] == R)) {
                    slashCaraFound = false
                } else {
                    slashCaraFound = false
                    if (this[i] != BACK_SLASH) {
                        result += this[i]
                    } else {
                        slashCaraFound = true
                    }
                }
            }
        }
        i++
    }
    return result
}

private fun String.toKgraphqlQuery() {
    val queryFirstIndex = this.indexOf(QUERY)
    if (queryFirstIndex > -1) {
        var queryLastIndex = this.indexOf(VARIABLES) - 1
        if (queryLastIndex < queryFirstIndex)
            queryLastIndex = this.length - 1
        run loop@{
            this.forEach { _ ->
                if (this[queryLastIndex] == DOUBLE_QUOTE || (this[queryLastIndex] == CURLY_BRACKETS_CLOSE && this[queryLastIndex - 1] != DOUBLE_QUOTE))
                    return@loop
                queryLastIndex--

            }
        }
        query = this.substring(queryFirstIndex + QUERY.length, queryLastIndex)
    }
}

private fun String.toKgraphqlVariables() {
    val queryFirstIndex = this.indexOf(VARIABLES)
    if (queryFirstIndex > -1) {
        var queryLastIndex = this.indexOf(QUERY) - 1
        if (queryLastIndex < queryFirstIndex)
            queryLastIndex = this.length - 1
        run loop@{
            this.forEach {_ ->
                if (this[queryLastIndex] == DOUBLE_QUOTE || (this[queryLastIndex] == CURLY_BRACKETS_CLOSE && this[queryLastIndex - 1] != DOUBLE_QUOTE))
                    return@loop
                queryLastIndex--

            }
        }
        variables = this.substring(queryFirstIndex + VARIABLES.length, queryLastIndex)
    }
    if (variables == NULL || variables == CURLY_BRACKETS_OPEN || variables == null || variables!!.isEmpty()) variables = null
}