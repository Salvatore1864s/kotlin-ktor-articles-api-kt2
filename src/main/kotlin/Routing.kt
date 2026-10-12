
package com.example

import com.example.data.ArticlesTable
import com.example.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
data class ArticleRequest(
    val title: String,
    val content: String
)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(mapOf("message" to "University API is running"))
        }

        // Регистрация
        post("/auth/register") {
            val request = call.receive<RegisterRequest>()

            if (request.login.isBlank() || request.password.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    AuthResponse("Логин и пароль не должны быть пустыми")
                )
                return@post
            }

            if (request.password.length < 6) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    AuthResponse("Пароль должен содержать минимум 6 символов")
                )
                return@post
            }

            val userId = AuthService.register(request.login, request.password)

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Conflict,
                    AuthResponse("Пользователь с таким логином уже существует")
                )
            } else {
                call.respond(
                    HttpStatusCode.Created,
                    AuthResponse("Регистрация успешна")
                )
            }
        }

        // Вход
        post("/auth/login") {
            val request = call.receive<LoginRequest>()
            val token = AuthService.login(request.login, request.password)

            if (token == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthResponse("Неверный логин или пароль")
                )
            } else {
                call.respond(
                    HttpStatusCode.OK,
                    AuthResponse("Вход выполнен успешно", token)
                )
            }
        }

        // Получить все статьи
        get("/articles") {
            val sort = call.request.queryParameters["sort"]

            val articles = transaction {
                val query = ArticlesTable.selectAll()

                when (sort) {
                    "new" -> query.orderBy(ArticlesTable.id, SortOrder.DESC).map {
                        Article(
                            it[ArticlesTable.id],
                            it[ArticlesTable.title],
                            it[ArticlesTable.content]
                        )
                    }

                    else -> query.orderBy(ArticlesTable.id, SortOrder.ASC).map {
                        Article(
                            it[ArticlesTable.id],
                            it[ArticlesTable.title],
                            it[ArticlesTable.content]
                        )
                    }
                }
            }

            call.respond(articles)
        }

        // Получить одну статью
        get("/articles/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный ID"))
                return@get
            }

            val article = transaction {
                ArticlesTable.selectAll()
                    .where { ArticlesTable.id eq id }
                    .singleOrNull()
                    ?.let {
                        Article(
                            it[ArticlesTable.id],
                            it[ArticlesTable.title],
                            it[ArticlesTable.content]
                        )
                    }
            }

            if (article == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Статья не найдена"))
            } else {
                call.respond(article)
            }
        }

        // Создание, изменение и удаление доступны только после входа
        authenticate("auth-jwt") {

            post("/articles") {
                val request = call.receive<ArticleRequest>()

                if (request.title.isBlank() || request.content.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Заголовок и содержание обязательны")
                    )
                    return@post
                }

                val article = transaction {
                    val id = ArticlesTable.insert {
                        it[title] = request.title
                        it[content] = request.content
                    } get ArticlesTable.id

                    Article(id, request.title, request.content)
                }

                call.respond(HttpStatusCode.Created, article)
            }

            put("/articles/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный ID"))
                    return@put
                }

                val request = call.receive<ArticleRequest>()

                if (request.title.isBlank() || request.content.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Заголовок и содержание обязательны")
                    )
                    return@put
                }

                val updated = transaction {
                    ArticlesTable.update({ ArticlesTable.id eq id }) {
                        it[title] = request.title
                        it[content] = request.content
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "Статья не найдена"))
                } else {
                    call.respond(
                        HttpStatusCode.OK,
                        Article(id, request.title, request.content)
                    )
                }
            }

            delete("/articles/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный ID"))
                    return@delete
                }

                val deleted = transaction {
                    ArticlesTable.deleteWhere { ArticlesTable.id eq id }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "Статья не найдена"))
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
