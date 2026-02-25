package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.repository.ProjectRepository
import org.springframework.scheduling.annotation.Scheduled
import java.math.BigDecimal
import java.sql.Timestamp

class hotnessScoreService (
    private val projectRepository: ProjectRepository
) {
    @Scheduled(cron = "0 0 * * * *")
    fun hotnessScoreCalculate() {
        val startDate = Timestamp(System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000))

        val maxAmount = projectRepository.getMaxDonationAmount(startDate) ?: BigDecimal.ONE
        val maxDonors = projectRepository.getMaxDonors(startDate) ?: 1L
        val maxComments = projectRepository.getMaxComments(startDate) ?: 1L

        val safeMaxAmount = if (maxAmount.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ONE else maxAmount
        val safeMaxDonors = if (maxDonors == 0L) 1L else maxDonors
        val safeMaxComments = if (maxComments == 0L) 1L else maxComments

        projectRepository.updateHotnessScore(startDate, safeMaxAmount, safeMaxDonors, safeMaxComments)
    }
}