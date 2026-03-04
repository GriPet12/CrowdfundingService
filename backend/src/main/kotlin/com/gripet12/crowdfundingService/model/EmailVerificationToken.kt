package com.gripet12.crowdfundingService.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_verification_tokens")
data class EmailVerificationToken(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val token: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val expiresAt: LocalDateTime = LocalDateTime.now().plusHours(24),

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
