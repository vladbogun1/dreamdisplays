package com.dreamdisplays.datatypes

import com.dreamdisplays.managers.DisplayManager.getDisplayData
import org.jspecify.annotations.NullMarked
import java.util.*

/**
 * Class to manage the state data of a display.
 *
 * @property id The unique identifier of the display.
 * @property displayData The display data.
 * @property paused Boolean indicating if the display is paused.
 * @property lastReportedTime The last reported time of the display.
 * @property lastReportedTimestamp The timestamp of the last report.
 * @property limitTime The limit time for the display.
 *
 * @param id The unique identifier of the display.
 *
 * @throws IllegalStateException if the display data is not found for the given ID.
 *
 */
@NullMarked
class StateData(private val id: UUID?) {
    // TODO: handle null id gracefully in the future
    // check(id != null) { "ID cannot be null" }
    // check(getDisplayData(id) != null) { "Display data not found for id: $id" }
    var displayData: DisplayData = getDisplayData(id)!!

    private var paused = false
    private var lastReportedTime: Long = 0
    private var lastReportedTimestamp: Long = 0
    private var limitTime: Long = 0
    private var volume: Float = 1.0f

    fun getCurrentTime(): Long {
        val nanos = System.nanoTime()
        val currentTime = if (paused) {
            lastReportedTime
        } else {
            lastReportedTime + (nanos - lastReportedTimestamp)
        }
        return if (limitTime > 0) currentTime % limitTime else currentTime
    }

    fun setPaused(value: Boolean) {
        lastReportedTime = getCurrentTime()
        lastReportedTimestamp = System.nanoTime()
        paused = value
    }

    fun seekRelative(seconds: Long) {
        val now = getCurrentTime()
        val shifted = now + (seconds * 1_000_000_000L)
        val clamped = shifted.coerceAtLeast(0)
        lastReportedTime = if (limitTime > 0) clamped % limitTime else clamped
        lastReportedTimestamp = System.nanoTime()
    }


    fun adjustVolume(step: Float) {
        val next = (volume + step).coerceIn(0.0f, 2.0f)
        volume = next
    }

    fun update(packet: SyncData) {
        paused = packet.currentState
        lastReportedTime = packet.currentTime
        lastReportedTimestamp = System.nanoTime()
        limitTime = packet.limitTime
        volume = packet.volume
    }

    fun createPacket(): SyncData {
        val currentTime = getCurrentTime()

        if (limitTime == 0L) displayData.duration?.let { limitTime = it }

        return SyncData(id, true, paused, currentTime, limitTime, volume)
    }
}
