package org.shaydee.item_descriptor.events

import com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL
import com.mojang.datafixers.util.Either
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.FormattedText
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.shaydee.item_descriptor.Screen.ItemDescriptorScreen
import org.shaydee.shaydeeapi.helpers.ClientHelpers
import org.shaydee.shaydeeapi.helpers.ColourHelpers
import org.shaydee.shaydeeapi.helpers.TextHelpers

object ClientEventHelpers {

    var toolTipTimer: Int = 0

    fun renderMoreInfoTooltip(
        item: ItemStack,
        current: MutableList<Either<FormattedText, TooltipComponent>>,
    ) {

        val instance = Minecraft.getInstance()
        val itemType = item.item
        if (itemType == Items.AIR) {
            toolTipTimer = 0
            return
        }

        val key = KEY_LCONTROL
        val holdToInfo = TextHelpers.displaySelectedKey("item_descriptor.text.l_control")
        val progress = toolTipTimer / 8
        val loading = "█".repeat(progress.coerceAtMost(5))

        val literal = TextHelpers.withStyleComponent(loading, ColourHelpers.colourByPercent(5, progress, true))

        current.add(Either.left(holdToInfo))
        if (loading.isNotEmpty()) current.add(Either.left(literal))

        if (ClientHelpers.isKeyDown(key)) toolTipTimer++ else toolTipTimer = 0

        if (progress >= 5) {
            instance.setScreen(ItemDescriptorScreen(item, instance.screen ?: return))
            toolTipTimer = 0
        }
    }

}