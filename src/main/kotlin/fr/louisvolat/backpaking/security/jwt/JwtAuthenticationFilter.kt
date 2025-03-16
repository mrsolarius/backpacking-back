package fr.louisvolat.backpaking.security.jwt

import fr.louisvolat.backpaking.service.auth.UserDetailsServiceImpl
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsServiceImpl,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        try {
            val jwt = getJwtFromRequest(request)

            if (!jwt.isNullOrBlank() && jwtTokenProvider.validateToken(jwt)) {
                try {
                    val username = jwtTokenProvider.getUsernameFromToken(jwt)
                    val userDetails = userDetailsService.loadUserByUsername(username)
                    val authentication = UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.authorities
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                } catch (e: Exception) {
                    logger.error("Could not set user authentication in security context", e)
                    // Ne pas interrompre la chaîne si l'authentification échoue
                }
            }

            // Continuer la chaîne de filtres
            chain.doFilter(request, response)
        } catch (e: Exception) {
            logger.error("Exception in JWT authentication filter", e)
            // En cas d'exception, continuez tout de même le traitement
            SecurityContextHolder.clearContext()
            chain.doFilter(request, response)
        }
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")

        return if (!bearerToken.isNullOrBlank() && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}