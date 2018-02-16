package y2k.experimentswithdatabasestructure

import android.database.sqlite.SQLiteDatabase
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import y2k.experimentswithdatabasestructure.eventsourcing.Api
import y2k.experimentswithdatabasestructure.eventsourcing.EventSourcing
import y2k.experimentswithdatabasestructure.eventsourcing.Events
import java.util.*

@RunWith(AndroidJUnit4::class)
class EventSourcingTests {

    /*

    User(id: UUID, email: String)
    |- books(id: UUID, title: String)
       |- bookmarks(id: UUID, page: Int)

     */

    @Test
    fun test() {
        val database = resetDatabase()
        database.execSQL("CREATE TABLE events (data TEXT)")

        val eventSourcing = EventSourcing<Events>()

        val id = UUID.randomUUID()
        eventSourcing.addEvent(Events.RegisterUser(id, "alice@net.net"), database, Api::executeEventInSql)
        eventSourcing.addEvent(Events.RegisterUser(UUID.randomUUID(), "bob@net.net"), database, Api::executeEventInSql)
        Assert.assertEquals(2, Api.selectUsers(database).size)

        eventSourcing.addEvent(Events.UnregisterUser(id), database, Api::executeEventInSql)
        Assert.assertEquals(1, Api.selectUsers(database).size)

        eventSourcing.resetTables(database, Api::executeEventInSql)
        Assert.assertEquals(1, Api.selectUsers(database).size)
    }

    private fun resetDatabase(): SQLiteDatabase = InstrumentationRegistry
        .getTargetContext()
        .getFileStreamPath("es-example.db")
        .apply { delete() }
        .let { SQLiteDatabase.openOrCreateDatabase(it, null) }
}