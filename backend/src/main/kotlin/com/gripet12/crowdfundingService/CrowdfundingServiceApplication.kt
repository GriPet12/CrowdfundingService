package com.gripet12.crowdfundingService

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.TimeZone

@SpringBootApplication
@EnableScheduling
class CrowdfundingServiceApplication

fun main(args: Array<String>) {
	TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
	runApplication<CrowdfundingServiceApplication>(*args)
}
