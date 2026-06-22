package com.gripet12.crowdfundingService.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class PostgresSequenceSync(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(PostgresSequenceSync::class.java)

    private val serialIdTables = listOf(
        "author_follows",
        "project_follows",
        "post_likes",
        "comments",
        "chat_messages",
        "analytics_logs"
    )

    override fun run(args: ApplicationArguments) {
        for (table in serialIdTables) {
            try {
                val seq = jdbcTemplate.queryForObject(
                    "SELECT pg_get_serial_sequence(?, 'id')",
                    String::class.java,
                    table
                )
                if (seq.isNullOrBlank()) continue

                val maxId = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) FROM $table",
                    Long::class.java
                ) ?: 0L

                val nextVal = jdbcTemplate.queryForObject(
                    "SELECT setval(?, GREATEST(?, 1), true)",
                    Long::class.java,
                    seq,
                    maxId
                )

                log.info("Synced sequence for {} -> next id {}", table, (nextVal ?: 0L) + 1)
            } catch (e: Exception) {
                log.warn("Could not sync sequence for {}: {}", table, e.message)
            }
        }
    }
}
