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

    private val serialIdTables = mapOf(
        "author_follows" to "id",
        "project_follows" to "id",
        "post_likes" to "id",
        "project_likes" to "id",
        "comments" to "comment_id",
        "chat_messages" to "message_id",
        "analytics_logs" to "log_id"
    )

    override fun run(args: ApplicationArguments) {
        ensureCommentsSchema()
        serialIdTables.forEach { (table, idColumn) -> syncTable(table, idColumn) }
    }

    fun ensureCommentsSchema() {
        try {
            jdbcTemplate.execute("ALTER TABLE comments ALTER COLUMN post_id DROP NOT NULL")
            log.info("comments.post_id is nullable")
        } catch (e: Exception) {
            log.debug("comments.post_id migration skipped: {}", e.message)
        }
        try {
            jdbcTemplate.execute("ALTER TABLE comments ADD COLUMN IF NOT EXISTS project_id BIGINT")
            log.info("comments.project_id column ensured")
        } catch (e: Exception) {
            log.warn("Could not ensure comments.project_id: {}", e.message)
        }
    }

    fun syncTable(table: String, idColumn: String) {
        try {
            val seq = jdbcTemplate.queryForObject(
                "SELECT pg_get_serial_sequence(?, ?)",
                String::class.java,
                table,
                idColumn
            )
            if (seq.isNullOrBlank()) {
                log.warn("No serial sequence found for {}.{}", table, idColumn)
                return
            }

            val maxId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX($idColumn), 0) FROM $table",
                Long::class.java
            ) ?: 0L

            val nextVal = jdbcTemplate.queryForObject(
                "SELECT setval(?, GREATEST(?, 1), true)",
                Long::class.java,
                seq,
                maxId
            )

            log.info("Synced sequence for {}.{} -> next id {}", table, idColumn, (nextVal ?: 0L) + 1)
        } catch (e: Exception) {
            log.warn("Could not sync sequence for {}.{}: {}", table, idColumn, e.message)
        }
    }
}
