package y2k.experimentswithdatabasestructure.eventsourcing

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import y2k.experimentswithdatabasestructure.common.asSequence
import y2k.experimentswithdatabasestructure.common.execute
import y2k.experimentswithdatabasestructure.common.query
import y2k.experimentswithdatabasestructure.common.transaction
import java.util.*

object Api {

    fun selectUsers(database: SQLiteDatabase): List<User> =
        database
            .query(SqlCommand("SELECT id, email FROM [users]")) {
                User(
                    id = UUID.fromString(it.getAsString("id")),
                    email = it.getAsString("email"))
            }

    fun eventToSql(event: Events): List<String> =
        when (event) {
            is Events.RegisterUser -> listOf(
                "CREATE TABLE IF NOT EXISTS [users] (id TEXT, email TEXT)",
                "INSERT INTO [users] VALUES ('${event.id}', '${event.email}')")
            is Events.UnregisterUser -> listOf(
                "CREATE TABLE IF NOT EXISTS [users] (id TEXT, email TEXT)",
                "DELETE FROM [users] WHERE id = '${event.id}'")
        }
}

sealed class Events {
    class RegisterUser(val id: UUID, val email: String) : Events()
    class UnregisterUser(val id: UUID) : Events()
}

class User(val id: UUID, val email: String)
data class SqlCommand(val sql: String, val args: List<String> = emptyList())

class EventSourcing<E> {

    private val serializer = Serializer<E>()

    fun addEvent(database: SQLiteDatabase, event: E, executeEventInSql: (E) -> List<String>) =
        SqlCommand("INSERT INTO events VALUES (?)", listOf(serializer.toString(event)))
            .let { listOf(it) }
            .plus(executeEventInSql(event).map { SqlCommand(it) })
            .let { commands ->
                database.transaction {
                    commands.forEach { database.execute(it) }
                }
            }

    fun reset(database: SQLiteDatabase, executeEventInSql: (E) -> List<String>) =
        database.transaction {
            database
                .query(
                    SqlCommand("SELECT name FROM sqlite_master WHERE type IN ('table') AND name <> 'events'"),
                    ::toDropCommand)
                .forEach { database.execute(it) }
            database
                .rawQuery("SELECT data FROM events ORDER BY rowid", emptyArray())
                .use {
                    it.asSequence { it.getString(0) }
                        .map(serializer::fromString)
                        .flatMap { event -> executeEventInSql(event).asSequence() }
                        .forEach(database::execSQL)
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