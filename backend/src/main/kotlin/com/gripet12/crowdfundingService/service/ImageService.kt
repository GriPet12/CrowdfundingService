package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.repository.ImageRepository
import com.gripet12.crowdfundingService.model.Image
import org.springframework.stereotype.Service

@Service
class ImageService(
    private val imageRepository: ImageRepository
) {
    fun saveImage(image: Image): Image {
        return try {
            imageRepository.save(image)
        } catch (e: Exception) {
            throw RuntimeException("Failed to save image: ${e.message}", e)
        }
    }

    fun getImage(id: Long): Image? {
        return imageRepository.findById(id).orElse(null)
    }

    fun deleteImage(id: Long) {
        imageRepository.deleteById(id)
    }
}