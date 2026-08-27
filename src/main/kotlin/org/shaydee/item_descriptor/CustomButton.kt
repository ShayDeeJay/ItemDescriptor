package org.shaydee.item_descriptor

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.FastColor
import org.shaydee.shaydeeapi.client.Icons
import org.shaydee.shaydeeapi.client.MultiIconType
import org.shaydee.shaydeeapi.helpers.ClientHelpers.icon
import org.shaydee.shaydeeapi.helpers.ClientHelpers.refinedTooltip
import org.shaydee.shaydeeapi.helpers.ColourHelpers
import org.shaydee.shaydeeapi.helpers.SoundHelpers

class CustomButton(
    pX: Int,
    pY: Int,
    private val defaultSize: Int,
    private val label: String,
    private val isSelected: Boolean,
    private val showHover: Boolean,
    private val canPress: Boolean,
    private val buttonOverlay: MultiIconType? = null,
    private val colour: Int = -1,
    private val onHover: (graphics: GuiGraphics, mouseX: Int, mouseY: Int) -> Unit = { a, b, c -> },
    private val pOnPress: OnPress
) : ImageButton(pX, pY, defaultSize, defaultSize, WidgetSprites(Icons.MENU_BUTTON.icon(), Icons.MENU_BUTTON.icon()), pOnPress) {

    private val totalSize: Int = defaultSize

    override fun isValidClickButton(button: Int): Boolean = canPress

    override fun onPress() = pOnPress.onPress(this)

    override fun playDownSound(handler: SoundManager) {
        if (canPress) SoundHelpers.uiSound(SoundEvents.UI_BUTTON_CLICK.value(), pitch = if(buttonOverlay != null) 1.5f else 0.5F)
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, pPartialTick: Float) {
        val mc = Minecraft.getInstance()

        if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble())) onHover(graphics, mouseX, mouseY)

        val colourFaded = if(colour == -1) FastColor.ARGB32.color(190, ColourHelpers.cosmicPurple) else colour
        val i1 = totalSize / 2 - 3

        renderNameAndBackground(graphics, mc, i1, colourFaded, mouseX, mouseY)
        showIconOrIndex(graphics, mc, i1)
        if (isMouseOver(mouseX.toDouble(), mouseY.toDouble())) hoverText(graphics, mouseX, mouseY, mc)
    }

    private fun showIconOrIndex(graphics: GuiGraphics, mc: Minecraft, i1: Int) {
        buttonOverlay?.let {
            val additionalSize = -2
            val expand = additionalSize / 2
            val adjustedSize = defaultSize + additionalSize

            MultiIconType.translatedIcon(it, graphics, adjustedSize, x - expand, y - expand)
            return
        }

        val textX = x + i1 + 3
        val textY = y + i1 - 1
        val color = ColourHelpers.headerColour
        graphics.drawCenteredString(mc.font, "text", textX, textY, color)
    }

    private fun renderNameAndBackground(graphics: GuiGraphics, mc: Minecraft, i1: Int, colourFaded: Int, mouseX: Int, mouseY: Int) {
        val color = if (isSelected) colourFaded else ColourHelpers.subHeaderColour
        val additionalSize = 6
        val expand = additionalSize / 2 // This is 6
        val adjustedSize = defaultSize + additionalSize

        graphics.icon(sprites.enabled(), adjustedSize, x - expand, y - expand)

        if(!showHover) graphics.drawCenteredString(mc.font, label, x + i1 + 3, y + (totalSize + 5), color)

        if (isSelected) {
            val inset = defaultSize/10

            graphics.renderOutline(
                (x - expand) + inset,
                (y - expand) + inset,
                adjustedSize - (inset * 2),
                adjustedSize - (inset * 2),
                colourFaded
            )
        }
    }

    private fun hoverText(graphics: GuiGraphics, mouseX: Int, mouseY: Int, mc: Minecraft) {
        if(!showHover) return

//        val get = when {
//             label.isNotEmpty() -> TextHelpers.withStyleComponentTrans(label, abilityColour)
//            else -> TextHelpers.withStyleComponentTrans("jahdoo_kotlin.text.non_assigned", ColourHelpers.headerColour, slotNumber)
//        }

//        if(get != Component.empty()) {
//            graphics.pose().translate(0F, 0F, 100F)
//            graphics.refinedTooltip(mouseX, mouseY + 10, mutableListOf(get))
//        }
    }


}