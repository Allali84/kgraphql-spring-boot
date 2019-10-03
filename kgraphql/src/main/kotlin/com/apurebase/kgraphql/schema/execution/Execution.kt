package com.apurebase.kgraphql.schema.execution

import com.apurebase.kgraphql.request.Arguments
import com.apurebase.kgraphql.request.OperationVariable
import com.apurebase.kgraphql.schema.directive.Directive
import com.apurebase.kgraphql.schema.structure2.Field
import com.apurebase.kgraphql.schema.structure2.Type


sealed class Execution {

    open class Node (
            val field: Field,
            val children: Collection<Execution>,
            val key : String,
            val alias: String? = null,
            val arguments : Arguments? = null,
            val typeCondition: TypeCondition? = null,
            val directives: Map<Directive, Arguments?>?,
            val variables: List<OperationVariable>?
    ) : Execution() {
        val aliasOrKey = alias ?: key
    }

    class Fragment(
            val condition: TypeCondition,
            val elements : List<Execution>,
            val directives: Map<Directive, Arguments?>?
    ) : Execution()

    class Union (
            val unionField: Field.Union<*>,
            val memberChildren: Map<Type, Collection<Execution>>,
            key: String,
            alias: String? = null,
            condition : TypeCondition? = null,
            directives: Map<Directive, Arguments?>? = null
    ) : Execution.Node (unionField, emptyList(), key, alias, null, condition, directives, null) {
        fun memberExecution(type: Type): Execution.Node {
            return Execution.Node (
                    field = field,
                    children = memberChildren[type] ?: throw IllegalArgumentException("Union ${unionField.name} has no member $type"),
                    key = key,
                    alias = alias,
                    arguments = arguments,
                    typeCondition = typeCondition,
                    directives = directives,
                    variables = null
            )
        }
    }
}