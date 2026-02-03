package com.gripet12.crowdfundingService.model

import com.gripet12.crowdfundingService.model.enums.Role
import jakarta.persistence.*

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

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = true) // <--- CHANGED TO TRUE
    val image: Image? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    val roles: Set<Role> = HashSet()
)