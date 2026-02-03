package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Video
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface VideoRepository : JpaRepository<Video, Long> {

    override fun findById(id: Long): Optional<Video>

    override fun deleteById(id: Long)
}