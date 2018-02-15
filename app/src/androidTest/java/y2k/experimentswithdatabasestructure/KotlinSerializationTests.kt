package y2k.experimentswithdatabasestructure

import android.support.test.runner.AndroidJUnit4
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KotlinSerializationTests {

    class Wrapper(val case: Cases)

    fun testJackson() {
        val case = Cases.Case1("my-key", "my-value")
        val json = mapper.writeValueAsString(Wrapper(case))
        Assert.assertEquals(
            """{"case":{"@class":"y2k.experimentswithdatabasestructure.KotlinSerializationTests${"$"}Cases${"$"}Case1","key":"my-key","value":"my-value"}}""",
            json)
        val actual = mapper.readValue<Wrapper>(json)
        Assert.assertEquals(case, actual.case)
    }

    @Test
    fun testJacksonCase2() {
        val holder = Wrapper(Cases.Case2("my-key"))
        Assert.assertEquals(holder.case, mapper.readValue<Wrapper>(mapper.writeValueAsString(holder)).case)
    }

    @Test
    @Ignore
    fun testJacksonCase3() {
        val holder = Wrapper(Cases.Case3)
        Assert.assertEquals(holder.case, mapper.readValue<Wrapper>(mapper.writeValueAsString(holder)).case)
    }

    @Test
    fun testSingleCase() {
        @Serializable
        data class SingleCase(val key: String, val value: String)

        Assert.assertEquals(
            """{"key":"my-key","value":"my-value"}""",
            JSON.stringify(SingleCase("my-key", "my-value")))
    }

    @Test
    fun testSingleDataCase() {
        @Serializable
        data class SingleDataCase(val key: String, val value: String)

        Assert.assertEquals(
            """{"key":"my-key","value":"my-value"}""",
            JSON.stringify(SingleDataCase("my-key", "my-value")))
    }

    @Test
    @Ignore
    fun test() {
        @Serializable
        class Holder(val case: Cases)

        val case1 = Holder(Cases.Case1("my-key", "my-value"))
        val serialized = JSON.stringify(case1)

        Assert.assertEquals("", serialized)
        val actual = JSON.parse<Cases>(serialized)
        Assert.assertEquals(case1, actual)
    }

    private val mapper = ObjectMapper()
        .registerKotlinModule()
        .enableDefaultTyping(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)

    @Serializable
    sealed class Cases {
        @Serializable
        data class Case1(val key: String, val value: String) : Cases()

        @Serializable
        data class Case2(val key: String) : Cases()

        @Serializable
        object Case3 : Cases()
    }
}