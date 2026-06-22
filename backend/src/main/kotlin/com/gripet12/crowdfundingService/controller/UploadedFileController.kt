package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.service.FileStorageService
import com.gripet12.crowdfundingService.service.ImagePreviewService
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRange
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/files")
class UploadedFileController(
    private val fileStorageService: FileStorageService,
    private val imagePreviewService: ImagePreviewService
) {
    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, Long?>> {
        val saved = fileStorageService.uploadFile(file)
        return ResponseEntity.ok(mapOf("id" to saved.id))
    }

    @GetMapping("/{id}/preview")
    fun getPreview(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "640") width: Int
    ): ResponseEntity<ByteArray> {
        val preview = imagePreviewService.getPreview(id, width) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(preview.mimeType))
            .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_IMMUTABLE)
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .contentLength(preview.bytes.size.toLong())
            .body(preview.bytes)
    }

    @GetMapping("/{id}")
    fun getFile(
        @PathVariable id: Long,
        @RequestHeader(value = HttpHeaders.RANGE, required = false) rangeHeader: String?
    ): ResponseEntity<ByteArray> {
        val file = fileStorageService.getFile(id) ?: return ResponseEntity.notFound().build()
        return buildFileResponse(file.data, file.mimeType, rangeHeader)
    }

    @GetMapping("/{id}/download")
    fun downloadFile(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val file = fileStorageService.getFile(id) ?: return ResponseEntity.notFound().build()
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(file.mimeType)
        headers.contentDisposition = ContentDisposition.attachment()
            .filename(file.originalFileName)
            .build()
        return ResponseEntity.ok()
            .headers(headers)
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .body(file.data)
    }

    private fun buildFileResponse(
        data: ByteArray,
        mimeType: String,
        rangeHeader: String?
    ): ResponseEntity<ByteArray> {
        val contentType = MediaType.parseMediaType(mimeType)
        val fileLength = data.size.toLong()

        if (rangeHeader.isNullOrBlank()) {
            return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_IMMUTABLE)
                .contentLength(fileLength)
                .body(data)
        }

        val ranges = HttpRange.parseRanges(rangeHeader)
        if (ranges.isEmpty()) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */$fileLength")
                .build()
        }

        val range = ranges.first()
        val start = range.getRangeStart(fileLength)
        val end = range.getRangeEnd(fileLength)
        val rangeLength = end - start + 1
        val partialData = data.copyOfRange(start.toInt(), (end + 1).toInt())

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .contentType(contentType)
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .header(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$fileLength")
            .contentLength(rangeLength)
            .body(partialData)
    }

    companion object {
        private const val CACHE_CONTROL_IMMUTABLE = "public, max-age=31536000, immutable"
    }
}
