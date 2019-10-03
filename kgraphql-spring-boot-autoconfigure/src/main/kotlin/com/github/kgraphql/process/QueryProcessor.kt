package com.github.kgraphql.process

const val QUERY = "\"query\":\""
const val VARIABLES = "\"variables\":"
const val DOUBLE_QUOTE = '"'
const val COMMA = ','
const val BACK_SLASH = '\\'
const val N = 'n'
const val T = 't'
const val R = 'r'
const val CURLY_BRACKETS_OPEN = '{'
const val CURLY_BRACKETS_CLOSE = '}'
const val HASH_SYMBOL = '#'
const val NULL = "null"

var variables: String? = ""
var queryWithoutComment = ""
var query = ""

fun process(request: String)  {
    queryWithoutComment = ""
    query = ""
    variables = ""
    request.removeAllComments()
    queryWithoutComment.toKgraphqlVariables()
    queryWithoutComment.toKgraphqlQuery()
}

fun String.removeAllComments() {
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
                        queryWithoutComment += this[i]
                    } else {
                        slashCaraFound = true
                    }
                }
            }
        }
        i++
    }
}

fun String.toKgraphqlQuery() {
    val queryFirstIndex = this.indexOf(QUERY)
    if (queryFirstIndex > -1) {
        //query = this.substring(queryFirstIndex + QUERY.length, this.length)
        var queryLastIndex = this.indexOf(VARIABLES) - 1
        if (queryLastIndex < queryFirstIndex)
            queryLastIndex = this.length - 1
        //var i = 0
        run loop@{
            this.forEach { _ ->
                if (this[queryLastIndex] == DOUBLE_QUOTE || (this[queryLastIndex] == CURLY_BRACKETS_CLOSE && this[queryLastIndex - 1] != DOUBLE_QUOTE))
                    return@loop
                queryLastIndex--
                /*when (it) {
                    CURLY_BRACKETS_OPEN -> i++
                    CURLY_BRACKETS_CLOSE -> {
                        i--
                        if (i == 0) {
                            if (query[queryLastIndex] == COMMA || query[queryLastIndex] == DOUBLE_QUOTE)
                                return@loop
                        }
                    }
                }*/

            }
        }
        query = this.substring(queryFirstIndex + QUERY.length, queryLastIndex)
    }
}

fun String.toKgraphqlVariables() {
    val queryFirstIndex = this.indexOf(VARIABLES)
    if (queryFirstIndex > -1) {
        //variables = this.substring(queryFirstIndex + VARIABLES.length, this.length)
        var queryLastIndex = this.indexOf(QUERY) - 1
        if (queryLastIndex < queryFirstIndex)
            queryLastIndex = this.length - 1
        //var i = 0
        run loop@{
            this.forEach {_ ->
                if (this[queryLastIndex] == DOUBLE_QUOTE || (this[queryLastIndex] == CURLY_BRACKETS_CLOSE && this[queryLastIndex - 1] != DOUBLE_QUOTE))
                    return@loop
                queryLastIndex--
                /*queryLastIndex++
                when (it) {
                    CURLY_BRACKETS_OPEN -> i++
                    CURLY_BRACKETS_CLOSE -> {
                        i--
                        if (i == 0) {
                            if (variables!![queryLastIndex] == COMMA || variables!![queryLastIndex] == DOUBLE_QUOTE)
                                return@loop
                        }
                    }
                }*/

            }
        }
        variables = this.substring(queryFirstIndex + VARIABLES.length, queryLastIndex)
    }
    if (variables == NULL || variables!!.isEmpty()) variables = null
}