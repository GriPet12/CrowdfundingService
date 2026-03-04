package com.gripet12.crowdfundingService.model

import com.gripet12.crowdfundingService.model.enums.Role
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userId: Long? = null,

    @Column(unique = true)
    val username: String,

    val password: String,

    val email: String,

    val isVerified: Boolean = false,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(columnDefinition = "boolean default false")
    val isPrivate: Boolean = false,

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = true) 
    val image: UploadedFile? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    val roles: MutableSet<Role> = HashSet(),

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    var banned: Boolean = false,

    @Column(name = "created_at", nullable = false,
        columnDefinition = "timestamp not null default now()")
    val createdAt: LocalDateTime = LocalDateTime.now()
)