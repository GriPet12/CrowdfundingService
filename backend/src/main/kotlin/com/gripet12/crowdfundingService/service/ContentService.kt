package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.ContentDto
import com.gripet12.crowdfundingService.model.Content
import com.gripet12.crowdfundingService.repository.ContentRepository

class ContentService (
    private val contentRepository: ContentRepository
) {
    fun createContent(contentType: String, mediaId: Long) {
        val content = Content(
            contentId = 0,
            contentType = contentType,
            mediaId = mediaId
        )
        contentRepository.save(content)

    }

    fun editContent(contentDto: ContentDto) {
        val contentToUpdate = contentRepository.findByContentId(contentDto.contentId)
            ?: throw IllegalArgumentException("Content with ID ${contentDto.contentId} not found")

        contentToUpdate.apply {
            contentType = contentDto.contentType
            mediaId = contentDto.mediaId
        }
        contentRepository.save(contentToUpdate)
    }

    fun deleteContent(contentId: Long) {
        contentRepository.deleteById(contentId)
    }

    fun getContent(contentId: Long): Content? = contentRepository.findByContentId(contentId)
}