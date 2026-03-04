package com.gripet12.crowdfundingService.config

import com.gripet12.crowdfundingService.security.JwtTokenFilter
import com.gripet12.crowdfundingService.security.OAuth2SuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtTokenFilter: JwtTokenFilter,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }

            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/ws/**").permitAll()
                    .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2.successHandler(oAuth2SuccessHandler)
            }

        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter::class.java)

        http.headers { headers ->
            headers.frameOptions { it.disable() }
        }

        return http.build()
    }

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager
}