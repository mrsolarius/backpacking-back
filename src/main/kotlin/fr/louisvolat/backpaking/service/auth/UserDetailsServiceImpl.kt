package fr.louisvolat.backpaking.service.auth

import fr.louisvolat.backpaking.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email: $email")

        // Pour simplifier, nous utilisons un simple rôle 'USER' pour tous les utilisateurs
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))

        return User(
            user.email,
            user.password,
            true,
            true,
            true,
            true,
            authorities
        )
    }

}