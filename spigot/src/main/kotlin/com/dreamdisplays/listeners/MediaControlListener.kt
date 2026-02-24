package com.dreamdisplays.listeners

import com.dreamdisplays.managers.DisplayManager.getDisplays
import com.dreamdisplays.managers.StateManager.seek
import com.dreamdisplays.managers.StateManager.togglePause
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.jspecify.annotations.NullMarked

/**
 * In-world media controls for displays.
 *
 * - Lever: play/pause toggle
 * - Button: seek (sneak = backward, normal = forward)
 */
@NullMarked
class MediaControlListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        val clicked = event.clickedBlock ?: return
        val action = event.action
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return

        val type = clicked.type
        if (!isControlBlock(type)) return

        val player = event.player
        val display = findNearbyDisplay(clicked) ?: return
        if (display.ownerId != player.uniqueId) return

        when {
            type == Material.LEVER -> {
                togglePause(display.id, player)
                event.isCancelled = true
            }

            isButton(type) -> {
                val direction = if (player.isSneaking) -5L else 5L
                seek(display.id, direction, player)
                event.isCancelled = true
            }
        }
    }

    private fun isControlBlock(type: Material): Boolean =
        type == Material.LEVER || isButton(type)

    private fun isButton(type: Material): Boolean = type.name.endsWith("_BUTTON")

    private fun findNearbyDisplay(block: Block) = getDisplays().firstOrNull { display ->
        if (display.pos1.world != block.world) return@firstOrNull false

        val x = block.x.toDouble() + 0.5
        val y = block.y.toDouble() + 0.5
        val z = block.z.toDouble() + 0.5

        val clampedX = x.coerceIn(display.box.minX - 1.0, display.box.maxX + 1.0)
        val clampedY = y.coerceIn(display.box.minY - 1.0, display.box.maxY + 1.0)
        val clampedZ = z.coerceIn(display.box.minZ - 1.0, display.box.maxZ + 1.0)

        val dx = x - clampedX
        val dy = y - clampedY
        val dz = z - clampedZ

        dx * dx + dy * dy + dz * dz <= 2.25 // within 1.5 blocks around display bounds
    }
}
