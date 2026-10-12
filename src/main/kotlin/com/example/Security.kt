
package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {
    val secret = System.getenv("JWT_SECRET")
        ?: "change-this-secret-before-production-please"

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "ktor-sample"

            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer("ktor-sample")
                    .withAudience("ktor-users")
                    .build()
            )

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt()

                if (userId != null && userId > 0) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("message" to "Требуется авторизация")
                )
            }
        }
    }
}
