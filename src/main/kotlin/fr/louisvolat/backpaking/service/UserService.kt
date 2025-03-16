package fr.louisvolat.backpaking.service

import fr.louisvolat.backpaking.model.User
import fr.louisvolat.backpaking.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun createUser(name: String, email: String, password: String): User {
        // Vérifier si l'email existe déjà
        require(userRepository.findByEmail(email) != null){"Email already exists"}

        // Encoder le mot de passe avant de l'enregistrer
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

    fun validatePassword(user: User, rawPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, user.password)
    }
}