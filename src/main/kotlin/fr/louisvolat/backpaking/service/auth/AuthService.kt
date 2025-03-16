package fr.louisvolat.backpaking.service.auth

import fr.louisvolat.backpaking.security.jwt.JwtTokenProvider
import fr.louisvolat.backpaking.service.UserService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider
) {
    fun authenticate(email: String, password: String): String {
        // Récupérer l'utilisateur
        val user = userService.getUserByEmail(email)
            ?: throw BadCredentialsException("Invalid email or password")

        // Vérifier le mot de passe
        if (!userService.validatePassword(user, password)) {
            throw BadCredentialsException("Invalid email or password")
        }

        // Créer les détails utilisateur Spring Security
        val userDetails = User(
            user.email,
            user.password,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        // Générer le token JWT
        return jwtTokenProvider.generateToken(userDetails)
    }
}