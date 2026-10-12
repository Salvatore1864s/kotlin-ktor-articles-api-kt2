
package com.example.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseFactory {
    fun init(): Database {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("DATABASE_URL")
                ?: "jdbc:postgresql://localhost:5432/ktor_app"
            username = System.getenv("DB_USER") ?: "ktor_user"
            password = System.getenv("DB_PASSWORD") ?: "ktor_password_123"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
        }

        return Database.connect(HikariDataSource(config))
    }
}
