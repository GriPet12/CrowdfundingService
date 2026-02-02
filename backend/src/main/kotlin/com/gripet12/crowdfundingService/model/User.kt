package com.gripet12.crowdfundingService.model

import com.gripet12.crowdfundingService.model.enums.Role
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.NoArgsConstructor

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userId: Long? = null,

    @Column(unique = true)
    val username: String,

    val password: String,

    val email: String,

    val isVerified: Boolean = false,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    val roles: Set<Role> = HashSet()
)