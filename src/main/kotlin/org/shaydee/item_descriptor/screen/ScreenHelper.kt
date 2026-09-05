package org.shaydee.item_descriptor.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.RecipeHolder
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.shaydee.item_descriptor.CustomButton
import org.shaydee.shaydeeapi.Helpers.cycleEntries
import org.shaydee.shaydeeapi.client.Icons
import org.shaydee.shaydeeapi.client.MultiIconType
import org.shaydee.shaydeeapi.helpers.ClientHelpers.customisableIcon
import org.shaydee.shaydeeapi.helpers.ClientHelpers.displayString
import org.shaydee.shaydeeapi.helpers.ClientHelpers.icon
import org.shaydee.shaydeeapi.helpers.ClientHelpers.refinedTooltip
import org.shaydee.shaydeeapi.helpers.ClientHelpers.stringWithBackground
import org.shaydee.shaydeeapi.helpers.ColourHelpers
import org.shaydee.shaydeeapi.helpers.ColourHelpers.netheriteBox
import org.shaydee.shaydeeapi.helpers.ColourHelpers.subHeaderColour
import org.shaydee.shaydeeapi.helpers.RenderHelpers.customItemRendererQ
import org.shaydee.shaydeeapi.helpers.TextHelpers.capsFirst

@OnlyIn(Dist.CLIENT)
object ScreenHelper {

    fun ItemDescriptorScreen.renderWidgets(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        for (renderable in this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick)
        }
    }

    fun ItemDescriptorScreen.renderDescription(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        mouseX: Int,
        mouseY: Int,
        mc: Minecraft
    ) {
        graphics.enableScissor(minX + 168, minY - 2, maxX + 169, maxY + 2)
        var spacer = 0
        descriptionComponents().forEach {
            graphics.displayString(it, x, ((y - shiftWithY - 12 + spacer) + panText).toInt())
            spacer += mc.font.lineHeight
        }
        descriptionScrollBar(mouseX, mouseY, graphics)
        graphics.flush()
        graphics.disableScissor()
    }

    fun GuiGraphics.renderRecipes(
        recipeType: List<RecipeHolder<*>>,
        x: Int,
        y: Int,
        mouseX: Int,
        mouseY: Int,
    ) {
        val mc = Minecraft.getInstance()
        val font = mc.font
        val startX = x + 54
        val level = mc.level ?: return
        val tickCount = mc.player?.tickCount ?: 0
        val current = recipeType.cycleEntries(tickCount)

        current?.let { recipe ->
            val name = current.value.type
            var itemCount = 0
            var rows = 0
            var spaceX = 0
            val empty = MutableList(9) { listOf(ItemStack.EMPTY) }

            recipe.value.ingredients.forEachIndexed { i, ingredient ->
                empty[i] = ingredient.items.toList()
            }

            val startY = y + 36
            val itemSize = 16

            empty.forEach {
                val x = startX + spaceX
                val y = startY + rows
                val itemStack = it.cycleEntries(tickCount, 20) ?: ItemStack.EMPTY
                customItemRendererQ(itemStack, x.toFloat(), y.toFloat())
                icon(Icons.MENU_BUTTON.icon(), 18, x - 1, y - 1)

                itemCount++
                spaceX += 20

                if (itemCount == 3) {
                    spaceX = 0
                    rows += 20
                    itemCount = 0
                }

                if (itemStack.item != Items.AIR) {
                    if (mouseX >= x && mouseX < x + itemSize && mouseY >= y && mouseY < y + itemSize) {
                        renderTooltip(font, itemStack, mouseX, mouseY)
                    }
                }
            }

            stringWithBackground(
                1F,
                Component.literal("${name.toString().capsFirst()} Recipe"),
                startX + 27,
                startY - 16,
                netheriteBox,
                subHeaderColour
            )
            customisableIcon(
                Icons.DIRECTION_ARROW.icon(),
                startX + 28,
                startY + 60,
                ColourHelpers.rating5Green,
                rotation = 180F
            )
            customItemRendererQ(
                recipe.value.toastSymbol,
                startX.toFloat() + 12,
                startY.toFloat() + 64,
                size = 32F
            )
            customisableIcon(
                Icons.DIRECTION_ARROW.icon(),
                startX + 28,
                startY + 102,
                ColourHelpers.rating2Red,
                rotation = 180F
            )

            val x1 = startX + 20
            val y1 = startY + 106
            val itemStack = recipe.value.getResultItem(level.registryAccess())
            if (mouseX >= x1 && mouseX < x1 + itemSize && mouseY >= y1 && mouseY < y1 + itemSize) {
                renderTooltip(font, itemStack, mouseX, mouseY)
            }
            customItemRendererQ(itemStack, x1.toFloat(), y1.toFloat())
            icon(Icons.MENU_BUTTON.icon(), 18, x1 - 1, y1 - 1)
            renderItemDecorations(font, itemStack, x1, y1)
        }
    }
}