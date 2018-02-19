package y2k.experimentswithdatabasestructure.common

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import kotlin.coroutines.experimental.buildSequence

sealed class Command
data class SqlCommand(val sql: String, val args: List<Any> = emptyList()) : Command()
class ListCommand(val items: List<Command>) : Command()
data class QueryCommand<out T>(val cmd: SqlCommand, val f: (ContentValues) -> T)

inline fun SQLiteDatabase.transaction(f: () -> Unit) {
    beginTransaction()
    try {
        f()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

fun SQLiteDatabase.execute(command: Command): Unit = when (command) {
    is SqlCommand -> execSQL(command.sql, command.args.toTypedArray())
    is ListCommand -> command.items.forEach { execute(it) }
}

fun <T> SQLiteDatabase.query(cmd: QueryCommand<T>): List<T> {
    val c = rawQuery(cmd.cmd.sql, cmd.cmd.args.map(Any::toString).toTypedArray())
    c.moveToPosition(-1)
    val result = ArrayList<T>()
    val cv = ContentValues()
    while (c.moveToNext()) {
        cv.clear()
        DatabaseUtils.cursorRowToContentValues(c, cv)
        result.add((cmd.f)(cv))
    }
    return result
}

inline fun <T> Cursor.asSequence(crossinline f: (Cursor) -> T): Sequence<T> =
    buildSequence {
        moveToPosition(-1)
        while (moveToNext())
            yield(f(this@asSequence))
    }