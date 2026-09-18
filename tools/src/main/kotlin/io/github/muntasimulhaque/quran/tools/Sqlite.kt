package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

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
