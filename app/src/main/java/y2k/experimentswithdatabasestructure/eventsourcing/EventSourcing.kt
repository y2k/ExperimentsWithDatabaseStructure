package y2k.experimentswithdatabasestructure.eventsourcing

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import y2k.experimentswithdatabasestructure.common.asSequence
import y2k.experimentswithdatabasestructure.common.queryList
import y2k.experimentswithdatabasestructure.common.toList
import java.util.*

object Api {

    @SuppressLint("Recycle")
    fun selectUsers(database: SQLiteDatabase): List<User> =
        database
            .rawQuery("SELECT id, email FROM [users]", emptyArray())
            .toList {
                User(
                    id = UUID.fromString(it.getString(0)),
                    email = it.getString(1))
            }

    fun executeEventInSql(event: Events) =
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

class EventSourcing<Events> {

    private val serializer = Serializer<Events>()

    fun addEvent(x: Events, database: SQLiteDatabase, executeEventInSql: (Events) -> List<String>) {
        database.execSQL("INSERT INTO events VALUES (?)", arrayOf(serializer.toString(x)))
        executeEventInSql(x, database, executeEventInSql)
    }

    private fun executeEventInSql(event: Events, database: SQLiteDatabase, executeEventInSql: (Events) -> List<String>) {
        executeEventInSql(event)
            .forEach { database.execSQL(it, emptyArray()) }
    }

    fun resetTables(database: SQLiteDatabase, executeEventInSql: (Events) -> List<String>) {
        database
            .queryList("SELECT NAME FROM sqlite_master WHERE type IN ('table') AND name <> 'events'") { it.getString(0) }
            .forEach { name -> database.execSQL("DROP TABLE $name") }
        database
            .rawQuery("SELECT data FROM events ORDER BY rowid", emptyArray())
            .use {
                it.asSequence { it.getString(0) }
                    .map(serializer::fromString)
                    .flatMap { event -> executeEventInSql(event).asSequence() }
                    .forEach(database::execSQL)
            }
    }

    private class Serializer<Events> {

        class Wrapper<out Events>(val x: Events)

        private val mapper = ObjectMapper()
            .registerKotlinModule()
            .enableDefaultTyping(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)

        fun toString(x: Events): String = mapper.writeValueAsString(Wrapper(x))
        fun fromString(x: String): Events = mapper.readValue<Wrapper<Events>>(x).x
    }
}