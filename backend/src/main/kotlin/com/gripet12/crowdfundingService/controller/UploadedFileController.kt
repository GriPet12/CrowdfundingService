package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.service.FileStorageService
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/files")
class UploadedFileController(
    private val fileStorageService: FileStorageService
) {
    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, Long?>> {
        val saved = fileStorageService.uploadFile(file)
        return ResponseEntity.ok(mapOf("id" to saved.id))
    }

    @GetMapping("/{id}")
    fun getFile(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val file = fileStorageService.getFile(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.mimeType))
            .body(file.data)
    }

    @GetMapping("/{id}/download")
    fun downloadFile(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val file = fileStorageService.getFile(id) ?: return ResponseEntity.notFound().build()
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(file.mimeType)
        headers.contentDisposition = ContentDisposition.attachment()
            .filename(file.originalFileName)
            .build()
        return ResponseEntity.ok().headers(headers).body(file.data)
    }
}
