package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.model.Video
import com.gripet12.crowdfundingService.repository.VideoRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class VideoService(
    private val videoRepository: VideoRepository
) {
    fun saveVideo(video: MultipartFile): Video {
        val video = Video(
            name = video.originalFilename ?: "unknown",
            data = video.bytes
        )
        return videoRepository.save(video)
    }

    fun getVideo(id: Long): Video? = videoRepository.findById(id).orElse(null)

    fun deleteVideo(id: Long) {
        videoRepository.deleteById(id)
    }
}