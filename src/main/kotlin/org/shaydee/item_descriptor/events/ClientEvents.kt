package org.shaydee.item_descriptor.events

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderHighlightEvent
import net.neoforged.neoforge.client.event.RenderTooltipEvent.GatherComponents
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import org.shaydee.shaydeeapi.helpers.ClientHelpers.centerX
import org.shaydee.shaydeeapi.helpers.ClientHelpers.centerY
import org.shaydee.shaydeeapi.helpers.ClientHelpers.refinedTooltip

@OnlyIn(Dist.CLIENT)
class ClientEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun tooltipEvent(e: GatherComponents) {
        ClientEventHelpers.renderMoreInfoTooltip(e.itemStack, e.tooltipElements)
    }

    @SubscribeEvent
    fun joinEvent(e: PlayerEvent.PlayerLoggedInEvent) {
        val player = e.entity
        val level = player.level()
        CreativeModeTabs.tryRebuildTabContents(
            level.enabledFeatures(),
            player.canUseGameMasterBlocks(),
            level.registryAccess()
        )
    }


}