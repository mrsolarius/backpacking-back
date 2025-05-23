package fr.louisvolat.backpaking.config

import fr.louisvolat.backpaking.security.jwt.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsFilter: CorsFilter,
    private val corsConfigurationSource: CorsConfigurationSource
) {
    @Value("\${app.upload.dir}")
    private lateinit var uploadDir: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/api/**")
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource)
            }
            .authorizeHttpRequests { auth ->
                // Routes accessibles à tous
                auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                auth.requestMatchers("/$uploadDir/**").permitAll()
                auth.requestMatchers("/api/**").permitAll() // Par défaut, toutes les routes API sont accessibles
                auth.anyRequest().denyAll()
                // Les routes spécifiques nécessitant une authentification seront gérées par @PreAuthorize
            }
            // Ajoutez le filtre CORS AVANT le filtre JWT
            .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}