package org.shaydee.item_descriptor.events

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderHighlightEvent
import net.neoforged.neoforge.client.event.RenderTooltipEvent.GatherComponents
import org.shaydee.shaydeeapi.helpers.ClientHelpers.centerX
import org.shaydee.shaydeeapi.helpers.ClientHelpers.centerY
import org.shaydee.shaydeeapi.helpers.ClientHelpers.refinedTooltip

@OnlyIn(Dist.CLIENT)
class ClientEvents {

    @SubscribeEvent
    fun tooltipEvent(e: GatherComponents) {
        ClientEventHelpers.renderMoreInfoTooltip(e.itemStack, e.tooltipElements)
    }

}