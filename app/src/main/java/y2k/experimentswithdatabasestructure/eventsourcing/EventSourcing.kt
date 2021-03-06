package y2k.experimentswithdatabasestructure.eventsourcing

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import y2k.experimentswithdatabasestructure.common.*
import java.util.*

object Api {

    fun queryUsers(): QueryCommand<User> =
        QueryCommand(
            SqlCommand("SELECT id, email FROM [users]"), {
            User(
                id = UUID.fromString(it.getAsString("id")),
                email = it.getAsString("email"))
        })

    fun eventToSql(event: Events): List<SqlCommand> =
        when (event) {
            is Events.RegisterUser -> listOf(
                SqlCommand("CREATE TABLE IF NOT EXISTS [users] (id TEXT, email TEXT)"),
                SqlCommand("INSERT INTO [users] VALUES (?, ?)", listOf(event.id, event.email)))
            is Events.UnregisterUser -> listOf(
                SqlCommand("CREATE TABLE IF NOT EXISTS [users] (id TEXT, email TEXT)"),
                SqlCommand("DELETE FROM [users] WHERE id = ?", listOf(event.id)))
            is Events.AddBook -> listOf(
                SqlCommand("CREATE TABLE IF NOT EXISTS [books] (id TEXT, bookId TEXT, title TEXT)"),
                SqlCommand("INSERT INTO [books] VALUES (?, ?, ?)", listOf(event.id, event.userId, event.title)))
            is Events.EditTitle -> listOf(
                SqlCommand("CREATE TABLE IF NOT EXISTS [books] (id TEXT, bookId TEXT, title TEXT)"),
                SqlCommand("UPDATE [books] SET title = ? WHERE id = ?", listOf(event.title, event.id)))
        }

    fun queryBooks(id: UUID?): QueryCommand<Book> =
        QueryCommand(
            SqlCommand("SELECT title FROM books"),
            { Book(title = it.getAsString("title")) })
}

sealed class Events {
    class RegisterUser(val id: UUID, val email: String) : Events()
    class UnregisterUser(val id: UUID) : Events()
    class AddBook(val userId: UUID, val id: UUID, val title: String) : Events()
    class EditTitle(val id: UUID, val title: String) : Events()
}

class Book(val title: String)
class User(val id: UUID, val email: String)

class EventSourcing<E> {

    private val serializer = Serializer<E>()

    fun addEvent(database: SQLiteDatabase, event: E, executeEventInSql: (E) -> List<SqlCommand>) =
        SqlCommand("INSERT INTO events VALUES (?)", listOf(serializer.toString(event)))
            .let { listOf(it) }
            .plus(executeEventInSql(event))
            .let { commands ->
                database.transaction {
                    commands.forEach { database.execute(it) }
                }
            }

    fun reset(database: SQLiteDatabase, executeEventInSql: (E) -> List<SqlCommand>) =
        database.transaction {
            QueryCommand(
                SqlCommand("SELECT name FROM sqlite_master WHERE type IN ('table') AND name <> 'events'"),
                ::toDropCommand)
                .let(database::query)
                .forEach { database.execute(it) }
            database
                .rawQuery("SELECT data FROM events ORDER BY rowid", emptyArray())
                .use {
                    it.asSequence { it.getString(0) }
                        .map(serializer::fromString)
                        .flatMap { event -> executeEventInSql(event).asSequence() }
                        .forEach(database::execute)
                }
        }

    private fun toDropCommand(cursor: ContentValues): SqlCommand =
        cursor.getAsString("name").let { SqlCommand("DROP TABLE $it") }

    private class Serializer<E> {

        class Wrapper<out E>(val x: E)

        private val mapper = ObjectMapper()
            .registerKotlinModule()
            .enableDefaultTyping(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)

        fun toString(x: E): String = mapper.writeValueAsString(Wrapper(x))
        fun fromString(x: String): E = mapper.readValue<Wrapper<E>>(x).x
    }
}