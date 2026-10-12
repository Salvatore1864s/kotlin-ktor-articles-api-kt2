
package com.example.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.data.UsersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

object AuthService {
    private val jwtSecret = System.getenv("JWT_SECRET")
        ?: "change-this-secret-before-production-please"

    private val jwtIssuer = "ktor-sample"
    private val jwtAudience = "ktor-users"

    fun register(login: String, password: String): Int? {
        if (login.isBlank() || password.isBlank()) return null

        return transaction {
            val existing = UsersTable.selectAll()
                .where { UsersTable.login eq login }
                .singleOrNull()

            if (existing != null) return@transaction null

            val hash = BCrypt.hashpw(password, BCrypt.gensalt())

            UsersTable.insert {
                it[UsersTable.login] = login
                it[passwordHash] = hash
            } get UsersTable.id
        }
    }

    fun login(login: String, password: String): String? {
        val user = transaction {
            UsersTable.selectAll()
                .where { UsersTable.login eq login }
                .singleOrNull()
                ?.let {
                    Triple(
                        it[UsersTable.id],
                        it[UsersTable.login],
                        it[UsersTable.passwordHash]
                    )
                }
        } ?: return null

        if (!BCrypt.checkpw(password, user.third)) return null

        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withSubject(user.first.toString())
            .withClaim("userId", user.first)
            .withClaim("login", user.second)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + 60 * 60 * 1000))
            .sign(Algorithm.HMAC256(jwtSecret))
    }
}
