package com.apurebase.kgraphql.integration

import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.extract
import com.apurebase.kgraphql.deserialize
import kotlinx.coroutines.delay
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import kotlin.random.Random

class ParallelExecutionTest {

    data class AType(val id: Int)

    private val syncResolversSchema = KGraphQL.schema {
        repeat(1000) {
            query("automated-$it") {
                resolver { ->
                    Thread.sleep(3)
                    "$it"
                }
            }
        }
    }

    private val suspendResolverSchema = KGraphQL.schema {
        repeat(1000) {
            query("automated-$it") {
                resolver { ->
                    delay(3)
                    "$it"
                }
            }
        }
    }

    private val suspendPropertySchema = KGraphQL.schema {
        query("getAll") {
            resolver { -> (0..999).map { AType(it) } }
        }
        type<AType> {
            property<List<AType>>("children") {
                resolver { parent ->
                    (0..50).map {
                        delay(Random.nextLong(1, 100))
                        AType((parent.id * 10) + it)
                    }
                }
            }
        }
    }

    @Test
    fun `Suspendable property resolvers`() {
        val query = "{getAll{id,children{id}}}"
        val map = deserialize(suspendPropertySchema.execute(query))

        MatcherAssert.assertThat(map.extract<Int>("data/getAll[0]/id"), CoreMatchers.equalTo(0))
        MatcherAssert.assertThat(map.extract<Int>("data/getAll[500]/id"), CoreMatchers.equalTo(500))
        MatcherAssert.assertThat(map.extract<Int>("data/getAll[766]/id"), CoreMatchers.equalTo(766))

        MatcherAssert.assertThat(map.extract<Int>("data/getAll[5]/children[5]/id"), CoreMatchers.equalTo(55))
        MatcherAssert.assertThat(map.extract<Int>("data/getAll[75]/children[9]/id"), CoreMatchers.equalTo(759))
        MatcherAssert.assertThat(map.extract<Int>("data/getAll[888]/children[50]/id"), CoreMatchers.equalTo(8930))
    }

    val query = "{ " + (0..999).map { "automated-${it}" }.joinToString(", ") + " }"

    @Test
    fun `1000 synchronous resolvers sleeping with Thread sleep`(){
        val map = deserialize(syncResolversSchema.execute(query))
        MatcherAssert.assertThat(map.extract<String>("data/automated-0"), CoreMatchers.equalTo("0"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-271"), CoreMatchers.equalTo("271"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-314"), CoreMatchers.equalTo("314"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-500"), CoreMatchers.equalTo("500"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-999"), CoreMatchers.equalTo("999"))
    }

    @Test
    fun `1000 suspending resolvers sleeping with suspending delay`(){
        val map = deserialize(suspendResolverSchema.execute(query))
        MatcherAssert.assertThat(map.extract<String>("data/automated-0"), CoreMatchers.equalTo("0"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-271"), CoreMatchers.equalTo("271"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-314"), CoreMatchers.equalTo("314"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-500"), CoreMatchers.equalTo("500"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-999"), CoreMatchers.equalTo("999"))
    }
}