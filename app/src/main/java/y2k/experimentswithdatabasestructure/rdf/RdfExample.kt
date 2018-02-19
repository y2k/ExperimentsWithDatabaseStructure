package y2k.experimentswithdatabasestructure.rdf

import y2k.experimentswithdatabasestructure.common.Command
import y2k.experimentswithdatabasestructure.common.ListCommand
import y2k.experimentswithdatabasestructure.common.QueryCommand
import y2k.experimentswithdatabasestructure.common.SqlCommand
import java.util.*

object RdfExample {

    data class User(val id: UUID, val name: String, val email: String)

    enum class Keys {
        Word,
        Translation,
        Selected
    }

    fun registerUser(id: UUID, name: String, email: String): Command =
        ListCommand(listOf(
            SqlCommand("INSERT INTO [users] VALUES ('$id', ':name', '$name')"),
            SqlCommand("INSERT INTO [users] VALUES ('$id', ':email', '$email')")))

    fun queryUsers(): QueryCommand<User> =
        QueryCommand(
            SqlCommand("""
                SELECT DISTINCT r.id AS id, r2.value AS name, r3.value AS email
                FROM [users] r
                JOIN [users] r2 ON r2.key = ':name' AND r2.id = r.id
                JOIN [users] r3 ON r3.key = ':email' AND r3.id = r.id
                """), {
            User(
                it.getAsString("id").let { UUID.fromString(it) },
                it.getAsString("name"),
                it.getAsString("email"))
        })
}