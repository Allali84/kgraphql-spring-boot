package com.apurebase.kgraphql.schema

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.RequestException
import com.apurebase.kgraphql.configuration.SchemaConfiguration
import com.apurebase.kgraphql.request.CachingDocumentParser
import com.apurebase.kgraphql.request.DocumentParser
import com.apurebase.kgraphql.request.VariablesJson
import com.apurebase.kgraphql.schema.execution.ParallelRequestExecutor
import com.apurebase.kgraphql.schema.execution.RequestExecutor
import com.apurebase.kgraphql.schema.introspection.__Schema
import com.apurebase.kgraphql.schema.structure2.LookupSchema
import com.apurebase.kgraphql.schema.structure2.RequestInterpreter
import com.apurebase.kgraphql.schema.structure2.SchemaModel
import com.apurebase.kgraphql.schema.structure2.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

class DefaultSchema (
        internal val configuration: SchemaConfiguration,
        internal val model : SchemaModel
) : Schema , __Schema by model, LookupSchema {

    companion object {
        const val OPERATION_NAME_PARAM = "operationName"
    }

    private val requestExecutor : RequestExecutor = ParallelRequestExecutor(this)

     private val requestInterpreter : RequestInterpreter = RequestInterpreter(model)

    /*
     * objects for request handling
     */
    private val documentParser = if(configuration.useCachingDocumentParser){
        CachingDocumentParser(configuration.documentParserCacheMaximumSize)
    } else {
        DocumentParser()
    }

    override fun execute(request: String, variables: String?, context: Context): String {
        val parsedVariables = variables
                ?.let { VariablesJson.Defined(configuration.objectMapper, variables) }
                ?: VariablesJson.Empty()
        val operations = documentParser.parseDocument(request)

        when(operations.size){
            0 -> {
                throw RequestException("Must provide any operation")
            }
            1 -> {
                return requestExecutor.execute(
                        plan = requestInterpreter.createExecutionPlan(operations.first()),
                        variables = parsedVariables,
                        context = context
                )
            }
            else -> {
                if(operations.any { it.name == null }){
                    throw RequestException("anonymous operation must be the only defined operation")
                } else {
                    val executionPlans = operations.associate { it.name to requestInterpreter.createExecutionPlan(it) }

                    val operationName = parsedVariables.get(String::class, String::class.starProjectedType, OPERATION_NAME_PARAM)
                            ?: throw RequestException("Must provide an operation name from: ${executionPlans.keys}")

                    val executionPlan = executionPlans[operationName]
                            ?: throw RequestException("Must provide an operation name from: ${executionPlans.keys}, found $operationName")

                    return requestExecutor.execute(executionPlan, parsedVariables, context)
                }
            }
        }
    }

    override fun typeByKClass(kClass: KClass<*>): Type? = model.queryTypes[kClass]

    override fun typeByKType(kType: KType): Type? = typeByKClass(kType.jvmErasure)

    override fun inputTypeByKClass(kClass: KClass<*>): Type? = model.inputTypes[kClass]

    override fun inputTypeByKType(kType: KType): Type? = typeByKClass(kType.jvmErasure)

    override fun typeByName(name: String): Type? = model.queryTypesByName[name]

    override fun inputTypeByName(name: String): Type? = model.inputTypesByName[name]
}