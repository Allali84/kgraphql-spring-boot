package com.apurebase.kgraphql.schema.dsl

import com.apurebase.kgraphql.schema.model.InputValueDef


class ResolverDSL(private val target: Target) {
    fun withArgs(block : InputValuesDSL.() -> Unit){
        val inputValuesDSL = InputValuesDSL(block)

        target.addInputValues(inputValuesDSL.inputValues.map { inputValue ->
            (inputValue.toKQLInputValue())
        })
    }

    interface Target {
        fun addInputValues(inputValues: Collection<InputValueDef<*>>)
    }
}