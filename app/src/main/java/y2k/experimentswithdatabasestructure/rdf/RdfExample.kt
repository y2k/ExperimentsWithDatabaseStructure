package y2k.experimentswithdatabasestructure.rdf

import java.util.*

@Suppress("unused")
object RdfExample {

    /*

    word        :   String
    translation :   String

     */

    sealed class Actions {
        class AddWord(val word: String, val translation: String)
    }

    enum class Keys {
        Word,
        Translation,
        Selected
    }

    private fun add(id: UUID, key: Keys, value: String): Unit = TODO()
    private fun remove(id: UUID, key: Keys): Unit = TODO()

    class Word(val word: String, val translation: String)

    fun addWord(word: String, translation: String): UUID {
        val id = UUID.randomUUID()
        add(id, Keys.Word, word)
        add(id, Keys.Translation, translation)
        return id
    }

    fun selectWord(id: UUID) = add(id, Keys.Selected, true.toString())
    fun unselect(id: UUID) = remove(id, Keys.Selected)

    fun querySelectedWord(): List<Word> = query("""
            select r.id, r2.value, r3.value
            from records r
            join records r2 on r2.key = ":word" and r2.id = r.id
            join records r3 on r3.key = ":translation" and r3.id = r.id
            where r.key = ":selected"
        """)

    private fun <T> query(s: String): List<T> = TODO()
}