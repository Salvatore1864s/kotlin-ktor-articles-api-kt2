
package com.example.data

import org.jetbrains.exposed.v1.core.Table

object ArticlesTable : Table("articles") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val content = text("content")

    override val primaryKey = PrimaryKey(id)
}

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val login = varchar("login", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)

    override val primaryKey = PrimaryKey(id)
}
