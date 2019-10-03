package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.model.FunctionWrapper
import org.junit.Test
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction

/**
 * Contrary to Java 8 which has about 43 different specialized function interfaces
 * to avoid boxing and unboxing as much as possible, the Function objects compiled by Kotlin
 * only implement fully generic interfaces, effectively using the Object type for any input or output value.
 *
 * This benchmark proves, that it makes no performance difference?
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 5)
open class FunctionExecutionBenchmark {

    @Param("true", "false")
    var useFunctionWrapper = true

    lateinit var functionWrapper : FunctionWrapper.ArityTwo<String, Int, String>

    lateinit var biFunction : BiFunction<Int, String, String>

    val arg1 = 3

    val arg2 = "CODE"

    val implementation = { int: Int, string: String -> "${int * ThreadLocalRandom.current().nextDouble()} $string" }

    @Setup
    fun setup(){
        if(useFunctionWrapper){
            functionWrapper = FunctionWrapper.ArityTwo(implementation, false)
        } else {
            biFunction = BiFunction(implementation)
        }
    }

    @Benchmark
    fun benchmarkFunctionExecution(): String? {
        if(useFunctionWrapper){
            return functionWrapper.invoke(arg1, arg2)
        } else {
            return biFunction.apply(arg1, arg2)
        }
    }

    @Test
    fun check(){
        setup()
        println(benchmarkFunctionExecution())
    }
}