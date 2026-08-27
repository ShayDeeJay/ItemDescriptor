package org.shaydee.item_descriptor.events

import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderTooltipEvent.GatherComponents

@OnlyIn(Dist.CLIENT)
class ClientEvents {


    @SubscribeEvent
    fun tooltipEvent(e: GatherComponents) {
        ClientEventHelpers.renderMoreInfoTooltip(e.itemStack, e.tooltipElements)
    }

}