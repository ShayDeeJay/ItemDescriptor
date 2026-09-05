package org.shaydee.item_descriptor.events

import com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL
import com.mojang.blaze3d.systems.RenderSystem.disableScissor
import com.mojang.blaze3d.systems.RenderSystem.enableScissor
import com.mojang.datafixers.util.Either
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.FormattedText
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.shaydee.item_descriptor.Helpers
import org.shaydee.item_descriptor.screen.ItemDescriptorScreen
import org.shaydee.shaydeeapi.helpers.ClientHelpers
import org.shaydee.shaydeeapi.helpers.ClientHelpers.displayString
import org.shaydee.shaydeeapi.helpers.ColourHelpers
import org.shaydee.shaydeeapi.helpers.TextHelpers


@OnlyIn(Dist.CLIENT)
object ClientEventHelpers {

    var toolTipTimer: Int = 0
    var previousItem: ItemStack = ItemStack.EMPTY

    fun renderMoreInfoTooltip(
        item: ItemStack,
        current: MutableList<Either<FormattedText, TooltipComponent>>,
    ) {
        if (item.`is`(Items.AIR)) {
            toolTipTimer = 0
            return
        }

        if(item != previousItem) toolTipTimer = 0
        this.previousItem = item

        val instance = Minecraft.getInstance()
        val screen = instance.screen
        if(screen is ItemDescriptorScreen && item == screen.codec) return

        val key = KEY_LCONTROL
        val holdToInfo = Helpers.displaySelectedKey("item_descriptor.text.l_control")
        val progress = toolTipTimer / 8
        val loading = "█".repeat(progress.coerceAtMost(5))
        val literal = TextHelpers.withStyleComponent(loading, ColourHelpers.colourByPercent(5, progress, true))

        current.add(Either.left(holdToInfo))

        if (loading.isNotEmpty()) current.add(Either.left(literal))
        if (ClientHelpers.isKeyDown(key)) toolTipTimer++ else toolTipTimer = 0
        if (progress >= 5) {
            instance.setScreen(ItemDescriptorScreen(item, screen ?: return))
            toolTipTimer = 0
        }
    }

}