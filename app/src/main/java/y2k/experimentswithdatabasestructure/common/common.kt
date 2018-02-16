package y2k.experimentswithdatabasestructure.common

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlin.coroutines.experimental.buildSequence

inline fun <T> SQLiteDatabase.queryList(sql: String, f: (Cursor) -> T): List<T> {
    val result = ArrayList<T>()
    val c = rawQuery(sql, emptyArray())
    c.moveToPosition(-1)
    while (c.moveToNext()) result += f(c)
    return result
}

inline fun <T> Cursor.toList(crossinline f: (Cursor) -> T): List<T> =
    use { it.asSequence(f).toList() }

inline fun <T> Cursor.asSequence(crossinline f: (Cursor) -> T): Sequence<T> =
    buildSequence {
        moveToPosition(-1)
        while (moveToNext())
            yield(f(this@asSequence))
    }