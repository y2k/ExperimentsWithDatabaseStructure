package y2k.experimentswithdatabasestructure.common

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import y2k.experimentswithdatabasestructure.eventsourcing.SqlCommand
import kotlin.coroutines.experimental.buildSequence

inline fun SQLiteDatabase.transaction(f: () -> Unit) {
    beginTransaction()
    try {
        f()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

fun SQLiteDatabase.execute(command: SqlCommand) =
    execSQL(command.sql, command.args.toTypedArray())

inline fun <T> SQLiteDatabase.query(command: SqlCommand, crossinline f: (ContentValues) -> T): List<T> {
    val c = rawQuery(command.sql, command.args.toTypedArray())
    c.moveToPosition(-1)

    val result = ArrayList<T>()
    val cv = ContentValues()
    while (c.moveToNext()) {
        DatabaseUtils.cursorRowToContentValues(c, cv)
        result.add(f(cv))
    }
    return result
}

inline fun <T> Cursor.asSequence(crossinline f: (Cursor) -> T): Sequence<T> =
    buildSequence {
        moveToPosition(-1)
        while (moveToNext())
            yield(f(this@asSequence))
    }