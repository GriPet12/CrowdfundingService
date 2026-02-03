package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.RewardDto
import com.gripet12.crowdfundingService.service.RewardService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("rewards")
class RewardController (
    private val rewardService: RewardService
) {

    @GetMapping("/{projectId}")
    fun getRewards(@PathVariable projectId: Long): List<RewardDto> =
        rewardService.getAllRewardsFromProject(projectId)

    @PostMapping
    fun addReward(@RequestBody rewardDto: RewardDto): Long =
        rewardService.addNewReward(rewardDto)

    @PutMapping("/{rewardId}")
    fun editReward(@PathVariable rewardId: Long, @RequestBody rewardDto: RewardDto) {
        rewardService.editReward(rewardId, rewardDto)
    }

    @DeleteMapping("/{rewardId}")
    fun deleteReward(@PathVariable rewardId: Long) {
        rewardService.deleteReward(rewardId)
    }

    @DeleteMapping("/{rewardId}/project/{projectId}")
    fun deleteRewardFromProject(@PathVariable rewardId: Long, @PathVariable projectId: Long) {
        rewardService.deleteProjectFromReward(rewardId)
    }

    @PostMapping("/{rewardId}/project/{projectId}")
    fun addRewardToProject(@PathVariable rewardId: Long, @PathVariable projectId: Long) {
        rewardService.addRewardToProject(rewardId, projectId)
    }
}