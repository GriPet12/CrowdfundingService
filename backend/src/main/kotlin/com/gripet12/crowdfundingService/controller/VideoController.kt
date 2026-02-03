package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.service.VideoService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("video")
class VideoController(val videoService: VideoService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadVideo(@RequestPart("video") video: MultipartFile): ResponseEntity<String> {
        return try {
            videoService.saveVideo(video)
            ResponseEntity.ok("Video saved to DB: ${video.originalFilename}")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Upload failed: ${e.message}")
        }
    }

    @GetMapping("/{id}")
    fun getVideo(@PathVariable id: Int): ResponseEntity<ByteArray> {
        val video = videoService.getVideo(id.toLong())
        return if (video != null) {
            ResponseEntity.ok(video.data)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
