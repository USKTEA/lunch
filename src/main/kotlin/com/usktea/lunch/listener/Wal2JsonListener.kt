package com.usktea.lunch.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.usktea.lunch.cdc.Wal2JsonV2Message
import com.usktea.lunch.common.logger
import org.postgresql.PGConnection
import org.postgresql.replication.LogSequenceNumber
import org.postgresql.replication.PGReplicationStream
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.sql.DriverManager
import java.util.Properties

abstract class Wal2JsonListener(
    private val baseUrl: String,
    private val user: String,
    private val password: String,
    private val objectMapper: ObjectMapper,
) {
    abstract val slotName: String

    abstract fun processCdcEvent(messages: List<Wal2JsonV2Message>)

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        Thread { listenLoop() }.start()
    }

    private fun listenLoop() {
        val cleanUrl = baseUrl.replace("jdbc:postgresql://", "")
        val hostPort = cleanUrl.substringBefore("/")
        val dbName = cleanUrl.substringAfter("/").substringBefore("?")

        val props =
            Properties().apply {
                setProperty("user", user)
                setProperty("password", password)
                setProperty("replication", "database")
                setProperty("preferQueryMode", "simple")
                setProperty("assumeMinServerVersion", "9.4")
            }

        val replicationUrl = "jdbc:postgresql://$hostPort/$dbName"

        DriverManager.getConnection(replicationUrl, props).use { conn ->
            val pgConn = conn.unwrap(PGConnection::class.java)

            val stream: PGReplicationStream =
                pgConn.replicationAPI
                    .replicationStream()
                    .logical()
                    .withSlotName(slotName)
                    .withStartPosition(LogSequenceNumber.INVALID_LSN)
                    .withSlotOption("format-version", 2)
                    .start()

            val buffer = mutableListOf<Wal2JsonV2Message>()

            while (true) {
                val msg: ByteBuffer? = stream.readPending()
                if (msg == null) {
                    Thread.sleep(50)
                    continue
                }

                val offset = msg.arrayOffset()
                val source = msg.array()
                val length = msg.remaining()

                if (length == 0) {
                    continue
                }

                val message =
                    objectMapper.readValue(
                        ByteArrayInputStream(source, offset, length),
                        Wal2JsonV2Message::class.java,
                    )

                if (message.isInsert || message.isDelete || message.isUpdate) {
                    buffer.add(message)
                }

                if (message.isCommit) {
                    try {
                        processCdcEvent(messages = buffer)
                    } catch (e: Exception) {
                        logger.error("Failed to process CDC event. buffer:{}", buffer, e)
                    } finally {
                        buffer.clear()
                    }
                }
                val lsn = stream.lastReceiveLSN
                stream.setAppliedLSN(lsn)
                stream.setFlushedLSN(lsn)
                stream.forceUpdateStatus()
            }
        }
    }
}
