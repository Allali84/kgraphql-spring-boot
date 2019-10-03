package com.apurebase.kgraphql.specification.language

import com.apurebase.kgraphql.RequestException
import com.apurebase.kgraphql.Specification
import com.apurebase.kgraphql.defaultSchema
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.expect
import com.apurebase.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test

@Specification("2.9 Input Values")
class InputValuesSpecificationTest {

    enum class FakeEnum{
        ENUM1, ENUM2
    }

    data class FakeData (val number : Int = 0, val description : String = "", val list : List<String> = emptyList())

    val schema = defaultSchema {
        enum<FakeEnum>()
        inputType<FakeData>()

        query("Int") { resolver { value: Int -> value } }
        query("Float") {resolver {value: Float -> value } }
        query("Double") {resolver {value: Double -> value } }
        query("String") {resolver {value: String -> value } }
        query("Boolean") {resolver {value: Boolean -> value } }
        query("Null") {resolver {value: Int? -> value } }
        query("Enum") {resolver {value: FakeEnum -> value } }
        query("List") {resolver {value: List<Int> -> value } }
        query("Object") {resolver {value: FakeData -> value.number } }
        query("ObjectList") {resolver {value: FakeData -> value.list } }
    }

    @Test
    @Specification("2.9.1 Int Value")
    fun `Int input value`(){
        val input = 4356
        val response = deserialize(schema.execute("{Int(value : $input)}"))
        assertThat(response.extract<Int>("data/Int"), equalTo(input))
    }

    @Test
    @Specification("2.9.2 Float Value")
    fun `Float input value`(){
        val input : Double = 4356.34
        val response = deserialize(schema.execute("{Float(value : $input)}"))
        assertThat(response.extract<Double>("data/Float"), equalTo(input))
    }

    @Test
    @Specification("2.9.2 Float Value")
    fun `Double input value`(){
        //GraphQL Float is Kotlin Double
        val input = 4356.34
        val response = deserialize(schema.execute("{Double(value : $input)}"))
        assertThat(response.extract<Double>("data/Double"), equalTo(input))
    }

    @Test
    @Specification("2.9.2 Float Value")
    fun `Double with exponential input value`(){
        val input = 4356.34e2
        val response = deserialize(schema.execute("{Double(value : $input)}"))
        assertThat(response.extract<Double>("data/Double"), equalTo(input))
    }

    @Test
    @Specification("2.9.4 String Value")
    fun `String input value`(){
        val input = "\\Ala ma kota \\\n\\kot ma Alę"
        val response = deserialize(schema.execute("{String(value : \"$input\")}"))
        assertThat(response.extract<String>("data/String"), equalTo(input))
    }

    @Test
    @Specification("2.9.3 Boolean Value")
    fun `Boolean input value`(){
        val input = true
        val response = deserialize(schema.execute("{Boolean(value : $input)}"))
        assertThat(response.extract<Boolean>("data/Boolean"), equalTo(input))
    }

    @Test
    @Specification("2.9.3 Boolean Value")
    fun `Invalid Boolean input value`(){
        expect<RequestException>("argument 'null' is not valid value of type Boolean"){
            deserialize(schema.execute("{Boolean(value : null)}"))
        }
    }

    @Test
    @Specification("2.9.5 Null Value")
    fun `Null input value`(){
        val response = deserialize(schema.execute("{Null(value : null)}"))
        assertThat(response.extract<Nothing?>("data/Null"), equalTo(null))
    }

    @Test
    @Specification("2.9.6 Enum Value")
    fun `Enum input value`(){
        val response = deserialize(schema.execute("{Enum(value : ENUM1)}"))
        assertThat(response.extract<String>("data/Enum"), equalTo(FakeEnum.ENUM1.toString()))
    }

    @Test
    @Specification("2.9.7 List Value")
    fun `List input value`(){
        val response = deserialize(schema.execute("{List(value : [23, 3, 23])}"))
        assertThat(response.extract<List<Int>>("data/List"), equalTo(listOf(23, 3, 23)))
    }

    @Test
    @Specification("2.9.8 Object Value")
    fun `Literal object input value`(){
        val response = deserialize(schema.execute("{Object(value: {number: 232, description: \"little number\"})}"))
        assertThat(
                response.extract<Int>("data/Object"),
                equalTo(232)
        )
    }

    @Test
    @Specification("2.9.8 Object Value")
    fun `Literal object input value with list field`(){
        val response = deserialize(schema.execute(
                "{ObjectList(" +
                        "value: {number: 232, " +
                        "description: \"little number\", " +
                        "list: [\"number\",\"description\",\"little number\"]})" +
                        "}"
        ))
        assertThat(
                response.extract<List<String>>("data/ObjectList"),
                equalTo(listOf("number", "description", "little number"))
        )
    }

    @Test
    @Specification("2.9.8 Object Value")
    fun `Object input value`(){
        val response = deserialize(schema.execute(
                "query(\$object: FakeData!){Object(value: \$object)}",
                "{ \"object\" : {\"number\":232, \"description\":\"little number\"}}"
        ))
        assertThat(response.extract<Int>("data/Object"), equalTo(232))
    }

    @Test
    @Specification("2.9.8 Object Value")
    fun `Object input value with list field`(){
        val response = deserialize(schema.execute(
                "query(\$object: FakeData!){ObjectList(value: \$object)}",
                "{ \"object\" : {\"number\":232, \"description\":\"little number\", \"list\" : [\"number\",\"description\",\"little number\"]}}"
        ))
        assertThat(
                response.extract<List<String>>("data/ObjectList"),
                equalTo(listOf("number", "description", "little number"))
        )
    }

    @Test
    @Specification("2.9.8 Object Value")
    fun `Unknown object input value type`(){
        expect<RequestException>("Invalid variable \$object argument type FakeDate, expected FakeData!"){
            schema.execute("query(\$object: FakeDate){Object(value: \$object)}")
        }
    }
}