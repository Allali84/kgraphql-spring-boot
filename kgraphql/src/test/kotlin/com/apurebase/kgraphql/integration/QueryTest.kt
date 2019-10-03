package com.apurebase.kgraphql.integration

import com.apurebase.kgraphql.RequestException
import com.apurebase.kgraphql.ValidationException
import com.apurebase.kgraphql.assertNoErrors
import com.apurebase.kgraphql.expect
import com.apurebase.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class QueryTest : BaseSchemaTest() {
    @Test
    fun `query nested selection set`(){
        val map = execute("{film{title, director{name, age}}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/title"), equalTo(prestige.title))
        assertThat(map.extract<String>("data/film/director/name"), equalTo(prestige.director.name))
        assertThat(map.extract<Int>("data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query collection field`(){
        val map = execute("{film{title, director{favActors{name, age}}}}")
        assertNoErrors(map)
        assertThat(map.extract<Map<String, String>>("data/film/director/favActors[0]"), equalTo(mapOf(
                "name" to prestige.director.favActors[0].name,
                "age" to prestige.director.favActors[0].age)
        ))
    }

    @Test
    fun `query scalar field`(){
        val map = execute("{film{id}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/id"), equalTo("${prestige.id.literal}:${prestige.id.numeric}"))
    }

    @Test
    fun `query with selection set on collection`(){
        val map = execute("{film{title, director{favActors{name}}}}")
        assertNoErrors(map)
        assertThat(map.extract<Map<String, String>>("data/film/director/favActors[0]"), equalTo(mapOf("name" to prestige.director.favActors[0].name)))
    }

    @Test
    fun `query with selection set on collection 2`(){
        val map = execute("{film{title, director{favActors{age}}}}")
        assertNoErrors(map)
        assertThat(map.extract<Map<String, Int>>("data/film/director/favActors[0]"), equalTo(mapOf("age" to prestige.director.favActors[0].age)))
    }

    @Test
    fun `query with invalid field name`(){
        expect<RequestException>("property favDish on Director does not exist"){
            execute("{film{title, director{name, favDish}}}")
        }
    }

    @Test
    fun `query with argument`(){
        val map = execute("{filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/filmByRank/title"), equalTo("Prestige"))
    }

    @Test
    fun `query with argument 2`(){
        val map = execute("{filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/filmByRank/title"), equalTo("Se7en"))
    }

    @Test
    fun `query with alias`(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/bestFilm/title"), equalTo("Prestige"))
    }

    @Test
    fun `query with field alias`(){
        val map =execute("{filmByRank(rank: 2){fullTitle: title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/filmByRank/fullTitle"), equalTo("Se7en"))
    }

    @Test
    fun `query with multiple aliases`(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}, secondBestFilm: filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/bestFilm/title"), equalTo("Prestige"))
        assertThat(map.extract<String>("data/secondBestFilm/title"), equalTo("Se7en"))
    }

    @Test
    fun `query with ignored property`(){
        expect<RequestException>("property author on Scenario does not exist"){
            execute("{scenario{author, content}}")
        }
    }

    @Test
    fun `query with interface`(){
        val map = execute("{randomPerson{name \n age}}")
        assertThat(map.extract<Map<String, String>>("data/randomPerson"), equalTo(mapOf(
                "name" to davidFincher.name,
                "age" to davidFincher.age)
        ))
    }

    @Test
    fun `query with collection elements interface`(){
        val map = execute("{people{name, age}}")
        assertThat(map.extract<Map<String, String>>("data/people[0]"), equalTo(mapOf(
                "name" to davidFincher.name,
                "age" to davidFincher.age)
        ))
    }

    @Test
    fun `query extension property`(){
        val map = execute("{actors{name, age, isOld}}")
        for(i in 0..4){
            val isOld = map.extract<Boolean>("data/actors[$i]/isOld")
            val age = map.extract<Int>("data/actors[$i]/age")
            assertThat(isOld, equalTo(age > 500))
        }
    }

    @Test
    fun `query extension property with arguments`(){
        val map = execute("{actors{name, picture(big: true)}}")
        for(i in 0..4){
            val name = map.extract<String>("data/actors[$i]/name").replace(' ', '_')
            assertThat(map.extract<String>("data/actors[$i]/picture"), equalTo("http://picture.server/pic/$name?big=true"))
        }
    }

    @Test
    fun `query extension property with optional argument`(){
        val map = execute("{actors{name, picture}}")
        for(i in 0..4){
            val name = map.extract<String>("data/actors[$i]/name").replace(' ', '_')
            assertThat(map.extract<String>("data/actors[$i]/picture"), equalTo("http://picture.server/pic/$name?big=false"))
        }
    }

    @Test
    fun `query with transformed property`(){
        val map = execute("{scenario{id, content(uppercase: false)}}")
        assertThat(map.extract<String>("data/scenario/content"), equalTo("Very long scenario"))

        val map2 = execute("{scenario{id, content(uppercase: true)}}")
        assertThat(map2.extract<String>("data/scenario/content"), equalTo("VERY LONG SCENARIO"))
    }

    @Test
    fun `query with invalid field arguments`(){
        expect<ValidationException>("Property id on type Scenario has no arguments, found: [uppercase]"){
            execute("{scenario{id(uppercase: true), content}}")
        }
    }

    @Test
    fun `query with external fragment`(){
        val map = execute("{film{title, ...dir }} fragment dir on Film {director{name, age}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/title"), equalTo(prestige.title))
        assertThat(map.extract<String>("data/film/director/name"), equalTo(prestige.director.name))
        assertThat(map.extract<Int>("data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query with nested external fragment`() {
        val map = execute("""
            {
                film {
                    title
                    ...dir
                }
            }

            fragment dir on Film {
                director {
                    name
                }
                ...dirAge
            }

            fragment dirIntermediate on Film {
                ...dirAge
            }

            fragment dirAge on Film {
                director {
                    age
                }
            }
            """.trimIndent())
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/title"), equalTo(prestige.title))
        assertThat(map.extract<String>("data/film/director/name"), equalTo(prestige.director.name))
        assertThat(map.extract<Int>("data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query with missing selection set`(){
        expect<RequestException>("Missing selection set on property film of type Film"){
            execute("{film}")
        }
    }
}