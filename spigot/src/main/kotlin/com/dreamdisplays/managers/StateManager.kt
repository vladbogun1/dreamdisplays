package com.dreamdisplays.managers

import com.dreamdisplays.datatypes.StateData
import com.dreamdisplays.datatypes.SyncData
import com.dreamdisplays.managers.DisplayManager.getDisplayData
import com.dreamdisplays.managers.DisplayManager.getReceivers
import com.dreamdisplays.utils.net.PacketUtils
import org.bukkit.entity.Player
import org.jspecify.annotations.NullMarked
import java.util.*

/**
 * Manages the state of displays being played by players.
 */
@NullMarked
object StateManager {
    private val playStates: MutableMap<UUID?, StateData> = HashMap()

    private fun getOrCreateState(id: UUID?): StateData? {
        val data = getDisplayData(id) ?: return null
        return playStates.computeIfAbsent(id) { StateData(id).also { state ->
            data.duration?.let {
                state.update(SyncData(id, data.isSync, false, 0L, it, 1.0f))
            }
        } }
    }

    @JvmStatic
    fun processSyncPacket(packet: SyncData, player: Player) {
        val data = getDisplayData(packet.id)
        if (data != null) data.isSync = packet.isSync

        if (!packet.isSync) {
            playStates.remove(packet.id)
            return
        }

        if (data == null) return

        if (data.ownerId != player.uniqueId) {
            return
        }

        val state = playStates.computeIfAbsent(packet.id) { id: UUID? -> StateData(id) }
        state.update(packet)
        data.duration = packet.limitTime

        val receivers = getReceivers(data)

        PacketUtils.sendSync(receivers.filter { it.uniqueId != player.uniqueId }.toMutableList(), packet)
    }

    @JvmStatic
    fun sendSyncPacket(id: UUID?, player: Player?) {
        val data = getDisplayData(id) ?: return
        if (!data.isSync) return

        val state = getOrCreateState(id)
        val packet = state?.createPacket() ?: SyncData(id, true, false, 0L, data.duration ?: 0L, 1.0f)

        PacketUtils.sendSync(mutableListOf(player), packet)
    }

    @JvmStatic
    fun togglePause(id: UUID?, actor: Player) {
        val data = getDisplayData(id) ?: return
        if (data.ownerId != actor.uniqueId) return

        data.isSync = true
        val state = getOrCreateState(id) ?: return
        val packetBefore = state.createPacket()
        state.setPaused(!packetBefore.currentState)
        val packet = state.createPacket()
        PacketUtils.sendSync(getReceivers(data).toMutableList(), packet)
    }

    @JvmStatic
    fun adjustVolume(id: UUID?, step: Float, actor: Player) {
        val data = getDisplayData(id) ?: return
        if (data.ownerId != actor.uniqueId) return

        data.isSync = true
        val state = getOrCreateState(id) ?: return
        state.adjustVolume(step)
        val packet = state.createPacket()
        PacketUtils.sendSync(getReceivers(data).toMutableList(), packet)
    }

    @JvmStatic
    fun seek(id: UUID?, seconds: Long, actor: Player) {
        val data = getDisplayData(id) ?: return
        if (data.ownerId != actor.uniqueId) return

        data.isSync = true
        val state = getOrCreateState(id) ?: return
        state.seekRelative(seconds)
        val packet = state.createPacket()
        PacketUtils.sendSync(getReceivers(data).toMutableList(), packet)
    }
}
