package y2k.experimentswithdatabasestructure

import android.database.sqlite.SQLiteDatabase
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import y2k.experimentswithdatabasestructure.common.query
import y2k.experimentswithdatabasestructure.eventsourcing.Api
import y2k.experimentswithdatabasestructure.eventsourcing.EventSourcing
import y2k.experimentswithdatabasestructure.eventsourcing.Events
import java.util.*

@RunWith(AndroidJUnit4::class)
class EventSourcingTests {

    /*
        User(id: UUID, email: String)
        |- books(id: UUID, title: String)
     */

    @Test
    fun test() {
        val db = resetDatabase()
        db.execSQL("CREATE TABLE events (data TEXT)")

        val eventSourcing = EventSourcing<Events>()

        val id = UUID.randomUUID()

        eventSourcing.addEvent(db, Events.RegisterUser(id, "alice@net.net"), Api::eventToSql)
        eventSourcing.addEvent(db, Events.RegisterUser(UUID.randomUUID(), "bob@net.net"), Api::eventToSql)
        Assert.assertEquals(2, Api.queryUsers().let(db::query).size)

        eventSourcing.addEvent(db, Events.UnregisterUser(id), Api::eventToSql)
        Assert.assertEquals(1, Api.queryUsers().let(db::query).size)

        //  The Adventures of Tom Sawyer
        val bookId = UUID.randomUUID()
        eventSourcing.addEvent(db, Events.AddBook(id, bookId, "The Adventures of Tom"), Api::eventToSql)
        Assert.assertEquals("The Adventures of Tom", Api.queryBooks(id).let(db::query).single().title)

        eventSourcing.addEvent(db, Events.EditTitle(bookId, "The Adventures of Tom Sawyer"), Api::eventToSql)
        Assert.assertEquals("The Adventures of Tom Sawyer", Api.queryBooks(id).let(db::query).single().title)

        eventSourcing.reset(db, Api::eventToSql)
        Assert.assertEquals(1, Api.queryUsers().let(db::query).size)
        Assert.assertEquals("The Adventures of Tom Sawyer", Api.queryBooks(id).let(db::query).single().title)
    }

    companion object {
        fun resetDatabase(): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(":memory:", null)
    }
}