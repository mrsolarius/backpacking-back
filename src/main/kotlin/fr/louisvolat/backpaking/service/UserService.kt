package fr.louisvolat.backpaking.service

import fr.louisvolat.backpaking.model.Travel
import fr.louisvolat.backpaking.model.User
import fr.louisvolat.backpaking.repository.TravelRepository
import fr.louisvolat.backpaking.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

private const val EMAIL_ALREADY_EXISTS = "Email already exists"

@Service
class UserService(
    private val userRepository: UserRepository,
    private val travelRepository: TravelRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun createUser(name: String, email: String, password: String): User {
        // Check if email already exists (corrected logic)
        require(userRepository.findByEmail(email) == null) { EMAIL_ALREADY_EXISTS }

        // Encode the password before saving
        val encodedPassword = passwordEncoder.encode(password)

        val user = User(
            name = name,
            email = email,
            password = encodedPassword,
            emailVerifiedAt = LocalDateTime.now()
        )

        return userRepository.save(user)
    }

    fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun getUserIdByEmail(email: String): Long? {
        return userRepository.findByEmail(email)?.id
    }

    fun getUserById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    fun validatePassword(user: User, rawPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, user.password)
    }

    fun getUserTravels(userId: Long): List<Travel> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        return user.travels
    }

    fun updateUser(id: Long, name: String?, email: String?, password: String?): User {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        if (name != null) user.name = name

        if (email != null && email != user.email) {
            // Check if new email already exists
            require(userRepository.findByEmail(email) == null) { EMAIL_ALREADY_EXISTS }
            user.email = email
        }

        if (password != null) {
            user.password = passwordEncoder.encode(password)
        }

        user.updatedAt = LocalDateTime.now()
        return userRepository.save(user)
    }

    fun deleteUser(id: Long): Boolean {
        val user = userRepository.findById(id).orElse(null) ?: return false

        // Note: We don't need to delete travels manually
        // as they are configured with CascadeType.ALL and orphanRemoval = true

        userRepository.delete(user)
        return true
    }
}