package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.model.Image
import com.gripet12.crowdfundingService.service.ImageService

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("images")
class ImageController(val imageService: ImageService) {

    @PostMapping
    fun uploadImage(@RequestParam("image") file: MultipartFile): ResponseEntity<String> {
        val image = Image(
            data = file.bytes
        )
        imageService.saveImage(image)
        return ResponseEntity.ok("Файл збережено!")
    }

    @GetMapping("/{id}")
    fun getImage(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val image = imageService.getImage(id)
            ?: throw RuntimeException("Image not found")

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(image.data)
    }
}
