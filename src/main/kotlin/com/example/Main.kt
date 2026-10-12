
package com.example

import com.example.config.DatabaseFactory
import com.example.data.ArticlesTable
import com.example.data.UsersTable
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun main() {
    DatabaseFactory.init()

    transaction {
        SchemaUtils.create(ArticlesTable, UsersTable)
    }

    embeddedServer(Netty, port = 8080) {
        configureSerialization()
        configureSecurity()
        configureRouting()
    }.start(wait = true)
}
