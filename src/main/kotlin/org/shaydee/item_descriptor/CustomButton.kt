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
import org.shaydee.shaydeeapi.helpers.ClientHelpers.alphaWrapper
import org.shaydee.shaydeeapi.helpers.ClientHelpers.boxMaker
import org.shaydee.shaydeeapi.helpers.ClientHelpers.icon
import org.shaydee.shaydeeapi.helpers.ClientHelpers.refinedTooltip
import org.shaydee.shaydeeapi.helpers.ColourHelpers
import org.shaydee.shaydeeapi.helpers.SoundHelpers
import org.shaydee.shaydeeapi.registry.SoundReg
import kotlin.text.Typography.half

class CustomButton(
    pX: Int,
    pY: Int,
    private val defaultSize: Int,
    private val isSelected: Boolean,
    private val showHover: Boolean,
    private val canPress: Boolean,
    private val buttonOverlay: MultiIconType? = null,
    private val label: String = "",
    private val colour: Int = -1,
    private val onHover: (graphics: GuiGraphics, mouseX: Int, mouseY: Int) -> Unit = { a, b, c -> },
    private val pOnPress: OnPress
) : ImageButton(pX, pY, defaultSize, defaultSize, WidgetSprites(Icons.MENU_BUTTON.icon(), Icons.MENU_BUTTON.icon()), pOnPress) {

    private val totalSize: Int = defaultSize

    override fun isValidClickButton(button: Int): Boolean = canPress

    override fun onPress() = pOnPress.onPress(this)

    override fun playDownSound(handler: SoundManager) {
        if (canPress) SoundHelpers.uiSound(SoundReg.INCREASE_SCORE.get(), pitch = if(buttonOverlay != null) 1.5f else 0.5F)
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, pPartialTick: Float) {
        val mc = Minecraft.getInstance()

        if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble())) onHover(graphics, mouseX, mouseY)

        val colourFaded = if(colour == -1) FastColor.ARGB32.color(190, ColourHelpers.cosmicPurple) else colour
        val i1 = totalSize / 2 - 3

        renderNameAndBackground(graphics, mc, i1, colourFaded, mouseX, mouseY)
        showIconOrIndex(graphics, mc, i1)
    }

    private fun showIconOrIndex(graphics: GuiGraphics, mc: Minecraft, i1: Int) {
        buttonOverlay?.let {
            when(buttonOverlay) {
                is MultiIconType.TextureIcon -> {
                    graphics.alphaWrapper(colour) { _ ->
                        MultiIconType.translatedIcon(it, graphics, defaultSize-2, x + 1, y + 1)
                    }
                }
                else -> MultiIconType.translatedIcon(it, graphics, defaultSize-2, x + 1, y + 1)
            }

            return
        }

        val textX = x + i1 + 3
        val textY = y + i1 - 1
        val color = ColourHelpers.headerColour
        graphics.drawCenteredString(mc.font, "text", textX, textY, color)
    }

    private fun renderNameAndBackground(graphics: GuiGraphics, mc: Minecraft, i1: Int, colourFaded: Int, mouseX: Int, mouseY: Int) {
        val color = if (isSelected) colourFaded else ColourHelpers.subHeaderColour
        graphics.icon(sprites.enabled(), defaultSize, x , y)

        if(!showHover) graphics.drawCenteredString(mc.font, label, x + i1 + 3, y + (totalSize + 5), color)
        if (isSelected) {
            val inset = defaultSize/10

            graphics.renderOutline(
                x + inset,
                y + inset,
                defaultSize - (inset * 2),
                defaultSize - (inset * 2),
                colourFaded
            )
        }

        if (isMouseOver(mouseX.toDouble(), mouseY.toDouble())) graphics.boxMaker(x, y , defaultSize/2, defaultSize/2,
            ColourHelpers.headerColour, 0)
    }


}