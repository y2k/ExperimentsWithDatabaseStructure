package y2k.experimentswithdatabasestructure.common

import android.database.Cursor
import kotlin.coroutines.experimental.buildSequence

inline fun <T> Cursor.useAsSequence(crossinline f: (Cursor) -> T): List<T> =
    use { it.asSequence(f).toList() }

inline fun <T> Cursor.asSequence(crossinline f: (Cursor) -> T): Sequence<T> =
    buildSequence {
        moveToPosition(-1)
        while (moveToNext())
            yield(f(this@asSequence))
    }