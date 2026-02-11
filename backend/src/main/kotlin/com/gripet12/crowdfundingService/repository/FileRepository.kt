package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.UploadedFile
import org.springframework.data.jpa.repository.JpaRepository

interface FileRepository : JpaRepository<UploadedFile, Long> {
}