package y2k.experimentswithdatabasestructure.eventsourcing

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import y2k.experimentswithdatabasestructure.common.useAsSequence
import y2k.experimentswithdatabasestructure.eventsourcing.EventSourcing.Events.RegisterUser
import y2k.experimentswithdatabasestructure.eventsourcing.EventSourcing.Events.UnregisterUser
import java.util.*
import kotlin.coroutines.experimental.buildSequence

object EventSourcing {

    sealed class Events {
        class RegisterUser(val id: UUID, val email: String) : Events()
        class UnregisterUser(val id: UUID) : Events()
    }

    object Api {

        @SuppressLint("Recycle")
        fun selectUsers(): List<User> =
            Sql.db
                .rawQuery("SELECT id, email FROM [users]", emptyArray())
                .useAsSequence {
                    User(
                        id = UUID.fromString(it.getString(0)),
                        email = it.getString(1))
                }
    }

    fun addEvent(x: Events) {
        Sql.execute("INSERT INTO events VALUES (?)", Serializer.toString(x))
        executeEventInSql(x)
    }

    private fun executeEventInSql(event: Events) {
        when (event) {
            is RegisterUser -> listOf(
                "CREATE TABLE IF NOT EXISTS [users] (id TEXT, email TEXT)",
                "INSERT INTO [users] VALUES ('${event.id}', '${event.email}')")
            is UnregisterUser -> listOf(
                "CREATE TABLE IF NOT EXISTS [users] (id TEXT, email TEXT)",
                "DELETE FROM [users] WHERE id = '${event.id}'")
        }.forEach { Sql.execute(it) }
    }

    @Suppress("unused")
    private fun restoreReadTables() {
        Sql.queryColumn("SELECT data FROM events ORDER BY rowid")
            .map { Serializer.fromString(it) }
            .forEach(::executeEventInSql)
    }

    object Serializer {

        class Wrapper(val x: Events)

        private val mapper = ObjectMapper()
            .registerKotlinModule()
            .enableDefaultTyping(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)

        fun toString(x: Events): String = mapper.writeValueAsString(Wrapper(x))
        fun fromString(x: String): Events = mapper.readValue<Wrapper>(x).x
    }

    object Sql {

        lateinit var db: SQLiteDatabase

        fun queryColumn(sql: String, vararg args: String): Sequence<String> {
            return buildSequence<String> {
                val cursor = db.rawQuery(sql, args)
                cursor.moveToPosition(-1)
                while (cursor.moveToNext())
                    yield(cursor.getString(0))
                cursor.close()
            }
        }

        fun execute(sql: String, vararg args: String) =
            db.execSQL(sql, args)
    }

    class User(val id: UUID, val email: String)
}