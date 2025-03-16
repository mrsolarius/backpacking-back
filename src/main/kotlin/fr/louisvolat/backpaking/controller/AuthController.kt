package fr.louisvolat.backpaking.controller

import fr.louisvolat.backpaking.dto.LoginRequest
import fr.louisvolat.backpaking.dto.LoginResponse
import fr.louisvolat.backpaking.dto.RegisterRequest
import fr.louisvolat.backpaking.service.auth.AuthService
import fr.louisvolat.backpaking.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        return try {
            val jwt = authService.authenticate(loginRequest.email, loginRequest.password)
            ResponseEntity.ok(LoginResponse(jwt))
        } catch (e: BadCredentialsException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid email or password"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Authentication failed"))
        }
    }

    // Endpoint accessible uniquement en localhost pour créer des utilisateurs
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody registerRequest: RegisterRequest,
        @RequestHeader("X-Forwarded-For", required = false) forwardedFor: String?,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<Any> {

        // Vérifier si la requête vient de localhost
        val clientIp = forwardedFor ?: request.remoteAddr
        if (clientIp != "127.0.0.1" && clientIp != "0:0:0:0:0:0:0:1" && clientIp != "localhost") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Access denied - only localhost registration allowed"))
        }

        return try {
            val user = userService.createUser(
                registerRequest.name,
                registerRequest.email,
                registerRequest.password
            )
            ResponseEntity.status(HttpStatus.CREATED)
                .body(mapOf(
                    "message" to "User registered successfully",
                    "userId" to user.id,
                    "email" to user.email
                ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Registration failed"))
        }
    }
}