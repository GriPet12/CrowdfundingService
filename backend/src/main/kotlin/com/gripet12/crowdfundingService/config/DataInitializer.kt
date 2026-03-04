package com.gripet12.crowdfundingService.config

import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.model.enums.Role
import com.gripet12.crowdfundingService.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${admin.username:admin}") private val adminUsername: String,
    @Value("\${admin.password:admin123}") private val adminPassword: String,
    @Value("\${admin.email:admin@crowdfunding.local}") private val adminEmail: String
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DataInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        val existingAdmin = userRepository.findAll()
            .any { it.roles.contains(Role.ROLE_ADMIN) }

        if (existingAdmin) {
            log.info("Admin user already exists — skipping default admin creation.")
            return
        }

        if (userRepository.existsByUsername(adminUsername)) {

            val user = userRepository.findByUsername(adminUsername).get()
            user.roles.add(Role.ROLE_ADMIN)
            userRepository.save(user)
            log.info("Granted ROLE_ADMIN to existing user '$adminUsername'.")
            return
        }

        val admin = User(
            username = adminUsername,
            password = passwordEncoder.encode(adminPassword),
            email = adminEmail,
            roles = mutableSetOf(Role.ROLE_ADMIN, Role.ROLE_USER)
        )
        userRepository.save(admin)
        log.info("========================================================")
        log.info("  Default admin account created:")
        log.info("  Username : $adminUsername")
        log.info("  Password : $adminPassword")
        log.info("  !! Change the password after first login !!")
        log.info("========================================================")
    }
}
