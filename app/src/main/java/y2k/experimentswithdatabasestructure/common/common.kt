package y2k.experimentswithdatabasestructure.common

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import y2k.experimentswithdatabasestructure.eventsourcing.QueryCommand
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