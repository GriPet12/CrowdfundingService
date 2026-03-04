package com.gripet12.crowdfundingService.security

import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.model.enums.Role
import com.gripet12.crowdfundingService.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OAuth2SuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${frontend.url:http://localhost:5173}") private val frontendUrl: String
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauthToken = authentication as OAuth2AuthenticationToken
        val oauthUser: OAuth2User = oauthToken.principal

        val email: String = oauthUser.getAttribute("email")
            ?: throw IllegalStateException("OAuth2 user has no email")

        val name: String = oauthUser.getAttribute<String>("name")
            ?: oauthUser.getAttribute<String>("login")
            ?: email.substringBefore("@")

        val user = userRepository.findAll().find { it.email == email }
            ?: run {

                var candidateUsername = name.replace(" ", "_").lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .take(40)
                if (candidateUsername.length < 4) candidateUsername = "user_${candidateUsername}"
                var username = candidateUsername
                var suffix = 1
                while (userRepository.existsByUsername(username)) {
                    username = "${candidateUsername}_${suffix++}"
                }
                userRepository.save(
                    User(
                        username = username,
                        password = passwordEncoder.encode(UUID.randomUUID().toString()),
                        email = email,
                        isVerified = true,
                        roles = mutableSetOf(Role.ROLE_USER)
                    )
                )
            }

        val roles = user.roles.toList()
        val token = jwtTokenProvider.createToken(user.username, roles)

        val redirectUrl = "$frontendUrl/oauth2/callback?token=$token" +
            "&id=${user.userId}&username=${user.username}" +
            "&role=${if (roles.contains(Role.ROLE_ADMIN)) "ADMIN" else "USER"}"

        response.sendRedirect(redirectUrl)
    }
}
