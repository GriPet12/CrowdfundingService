package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.AuthorChatSettings
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorChatSettingsRepository : JpaRepository<AuthorChatSettings, Long>
