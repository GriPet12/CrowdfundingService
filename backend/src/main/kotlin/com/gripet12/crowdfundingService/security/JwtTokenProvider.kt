package com.gripet12.crowdfundingService.security

import com.gripet12.crowdfundingService.config.JwtConfig
import com.gripet12.crowdfundingService.model.Role
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtConfig: JwtConfig,
    private val userDetailsService: UserDetailsServiceImpl
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtConfig.secret.toByteArray())

    fun createToken(username: String, roles: List<Role>): String {
        val now = Date()
        val validity = Date(now.time + jwtConfig.expirationMs)

        return Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(validity)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload

            return !claims.expiration.before(Date())
        } catch (e: Exception) {
            return false
        }
    }

    fun getAuthentication(token: String): Authentication {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        val username = claims.subject
        val roles = claims["roles"] as List<*>

        val authorities = roles.map { SimpleGrantedAuthority(it.toString()) }
        val userDetails = userDetailsService.loadUserByUsername(username)

        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
    }

    fun getUsername(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }
}