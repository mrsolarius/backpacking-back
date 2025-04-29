package fr.louisvolat.backpaking.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig {

    @Value("\${app.upload.dir:uploads}")
    private lateinit var uploadDir: String

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Autorisez votre domaine frontend
        configuration.allowedOrigins = listOf("https://backpaking.louisvolat.fr")

        // Méthodes HTTP autorisées
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")

        // En-têtes autorisés
        configuration.allowedHeaders = listOf("Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With")

        // Autoriser les cookies et auth
        configuration.allowCredentials = true

        // Exposer certains en-têtes à l'application cliente
        configuration.exposedHeaders = listOf("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials")

        // Mettre en cache pour 1 heure (3600 secondes)
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()

        // Configuration pour les endpoints API
        source.registerCorsConfiguration("/api/**", configuration)

        // Configuration pour les uploads
        source.registerCorsConfiguration("/uploads/**", configuration)
        source.registerCorsConfiguration("/$uploadDir/**", configuration)

        return source
    }

    @Bean
    fun corsFilter(corsConfigurationSource: CorsConfigurationSource): CorsFilter {
        return CorsFilter(corsConfigurationSource)
    }
}