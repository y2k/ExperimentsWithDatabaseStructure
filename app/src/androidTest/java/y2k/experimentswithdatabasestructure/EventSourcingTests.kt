package y2k.experimentswithdatabasestructure

import android.database.sqlite.SQLiteDatabase
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import y2k.experimentswithdatabasestructure.eventsourcing.EventSourcing
import y2k.experimentswithdatabasestructure.eventsourcing.EventSourcing.Api
import y2k.experimentswithdatabasestructure.eventsourcing.EventSourcing.Events
import y2k.experimentswithdatabasestructure.eventsourcing.EventSourcing.Sql
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
        Sql.db = resetDatabase()
        Sql.db.execSQL("CREATE TABLE events (data TEXT)")

        val id = UUID.randomUUID()
        EventSourcing.addEvent(Events.RegisterUser(id, "alice@net.net"))
        EventSourcing.addEvent(Events.RegisterUser(UUID.randomUUID(), "bob@net.net"))
        Assert.assertEquals(2, Api.selectUsers().size)

        EventSourcing.addEvent(Events.UnregisterUser(id))
        Assert.assertEquals(1, Api.selectUsers().size)
    }

    private fun resetDatabase(): SQLiteDatabase = InstrumentationRegistry
        .getTargetContext()
        .getFileStreamPath("es-example.db")
        .apply { delete() }
        .let { SQLiteDatabase.openOrCreateDatabase(it, null) }
}