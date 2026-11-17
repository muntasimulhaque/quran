package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * The pipeline's one door to SQLite: the driver the build tasks open their
 * scratch files with, and the three reads every gate makes of a built
 * database.
 *
 * These are the build's own books and never the app's: the app reads the
 * content database through the SQLite the platform carries. The table name a
 * gate counts rows in is a contract of the build's own shape, so a change to
 * what the builder writes is a change to the gates beside it.
 */
fun openSqlite(file: File): Connection {
    Class.forName("org.sqlite.JDBC")
    return DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
}

fun Connection.count(table: String): Long =
    createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
            rs.next()
            rs.getLong(1)
        }
    }

fun Connection.scalarLong(sql: String, vararg args: String): Long =
    prepareStatement(sql).use { statement ->
        args.forEachIndexed { index, value -> statement.setString(index + 1, value) }
        statement.executeQuery().use { rs ->
            rs.next()
            rs.getLong(1)
        }
    }

fun Connection.each(sql: String, row: (java.sql.ResultSet) -> Unit) {
    createStatement().use { statement ->
        statement.executeQuery(sql).use { rs ->
            while (rs.next()) row(rs)
        }
    }
}
