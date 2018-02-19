package y2k.experimentswithdatabasestructure

import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert
import org.junit.Test
import org.junit.runner.RunWith
import y2k.experimentswithdatabasestructure.EventSourcingTests.Companion.resetDatabase
import y2k.experimentswithdatabasestructure.common.execute
import y2k.experimentswithdatabasestructure.common.query
import y2k.experimentswithdatabasestructure.rdf.RdfExample
import java.util.*

@RunWith(AndroidJUnit4::class)
class RdfTests {

    /*
        User(id: UUID, email: String)
        |- books(id: UUID, title: String)
     */

    @Test
    fun test() {
        val db = resetDatabase()
        db.execSQL("CREATE TABLE [users] (id TEXT, key TEXT, value TEXT)")
        db.execSQL("CREATE TABLE [books] (id TEXT, key TEXT, value TEXT)")

        val id = UUID.randomUUID()
        RdfExample.registerUser(id, "Alice", "alice@net.net").let(db::execute)
        val id2 = UUID.randomUUID()
        RdfExample.registerUser(id2, "Bob", "bob@net.net").let(db::execute)

        Assert.assertEquals(
            listOf(
                RdfExample.User(id, "Alice", "alice@net.net"),
                RdfExample.User(id2, "Bob", "bob@net.net")),
            RdfExample.queryUsers().let { db.query(it) })
    }
}