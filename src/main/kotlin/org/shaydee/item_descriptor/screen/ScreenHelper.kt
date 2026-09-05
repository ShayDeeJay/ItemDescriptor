package org.shaydee.item_descriptor.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.commands.arguments.ResourceLocationArgument.getRecipe
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder.smithing
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs.collection
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.SmithingTransformRecipe
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.shaydee.shaydeeapi.Helpers.cycleEntries
import org.shaydee.shaydeeapi.client.Icons
import org.shaydee.shaydeeapi.helpers.ClientHelpers.customisableIcon
import org.shaydee.shaydeeapi.helpers.ClientHelpers.displayString
import org.shaydee.shaydeeapi.helpers.ClientHelpers.icon
import org.shaydee.shaydeeapi.helpers.ClientHelpers.renderTooltipFromPos
import org.shaydee.shaydeeapi.helpers.ClientHelpers.stringWithBackground
import org.shaydee.shaydeeapi.helpers.ColourHelpers
import org.shaydee.shaydeeapi.helpers.ColourHelpers.netheriteBox
import org.shaydee.shaydeeapi.helpers.ColourHelpers.subHeaderColour
import org.shaydee.shaydeeapi.helpers.RenderHelpers.customItemRenderer
import org.shaydee.shaydeeapi.helpers.TextHelpers

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
        screen: ItemDescriptorScreen,
        recipeType: List<RecipeHolder<*>>,
        x: Int,
        y: Int,
        mouseX: Int,
        mouseY: Int,
    ) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val tickCount = mc.player?.tickCount ?: 0
        val current = recipeType.cycleEntries(tickCount)

        current?.let { recipe ->
            val getRecipe = current.value

            val isSmithing = getRecipe is SmithingTransformRecipe
            val font = mc.font
            val startX = x + 54
            val startY = y + screen.shiftWithY + if(isSmithing) -2 else 18
            var itemCount = 0
            var rows = 0
            var spaceX = 0
            val crafting = MutableList(9) { emptyList<ItemStack>() }
            val smithing = MutableList(3) { emptyList<ItemStack>() }

            recipeRenderType(getRecipe, crafting, smithing)

            icon(Icons.MENU_BUTTON.icon(), 36, startX + 12 - 2, startY + 64 - 2)

            stringWithBackground(
                1F,
                Component.literal(TextHelpers.stringIdToName(getRecipe.type.toString())),
                startX + 27,
                startY - if(isSmithing) -24 else 16,
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

            customItemRenderer(
                recipe.value.toastSymbol,
                startX + 12,
                startY + 64,
                size = 32F
            )

            customisableIcon(
                Icons.DIRECTION_ARROW.icon(),
                startX + 28,
                startY + 101,
                ColourHelpers.rating2Red,
                rotation = 180F
            )

            fun List<List<ItemStack>>.boxes(isSmithing: Boolean){
                forEach {
                    val x = startX + spaceX
                    val y = startY + rows + if(isSmithing) +40 else 0
                    val itemStack = it.cycleEntries(tickCount, 20) ?: ItemStack.EMPTY
                    customItemRenderer(itemStack, x, y)
                    icon(Icons.MENU_BUTTON.icon(), 18, x - 1, y - 1)

                    spaceX += 20

                    if (!isSmithing) {
                        itemCount++
                        if (itemCount == 3) {
                            spaceX = 0
                            rows += 20
                            itemCount = 0
                        }
                    }

                    if (itemStack.item == Items.AIR) return@forEach
                    renderTooltipFromPos(mouseX, mouseY, x, y, font, itemStack)
                }
            }

            when(getRecipe) {
                is SmithingTransformRecipe -> smithing.boxes(true)
                else -> crafting.boxes(false)
            }

            val x1 = startX + 20
            val y1 = startY + 105
            val itemStack = recipe.value.getResultItem(level.registryAccess())
            renderTooltipFromPos(mouseX, mouseY, x1, y1, font, itemStack)
            customItemRenderer(itemStack, x1, y1)
            icon(Icons.MENU_BUTTON.icon(), 18, x1 - 1, y1 - 1)
            renderItemDecorations(font, itemStack, x1, y1)
        }
    }

    private fun recipeRenderType(
        getRecipe: Recipe<*>,
        crafting: MutableList<List<ItemStack>>,
        smithing: MutableList<List<ItemStack>>,
    ) {
        when (getRecipe) {
            is ShapedRecipe -> {
                getRecipe.ingredients.forEachIndexed { index, ingredient ->
                    val width = getRecipe.width
                    val height = getRecipe.height
                    val recipeX = index % width
                    val recipeY = index / width
                    val offsetX = (3 - width) / 2
                    val offsetY = (3 - height) / 2
                    val x = recipeX + offsetX
                    val y = recipeY + offsetY
                    crafting[y * 3 + x] = ingredient.items.toList()
                }
            }

            is SmithingTransformRecipe -> {
                val smithRecipe = getRecipe.smithingIngredientsReflective()
                smithing[0] = smithRecipe[0].items.toList()
                smithing[1] = smithRecipe[1].items.toList()
                smithing[2] = smithRecipe[2].items.toList()
            }

            else -> {
                getRecipe.ingredients.forEachIndexed { index, ingredient ->
                    crafting[index] = ingredient.items.toList()
                }
            }
        }
    }

    private fun SmithingTransformRecipe.smithingIngredientsReflective(): List<Ingredient> =
        listOf("template", "base", "addition").mapNotNull { name ->
            val field = SmithingTransformRecipe::class.java.getDeclaredField(name)
                .apply { isAccessible = true }
            when (val value = field.get(this)) {
                is Ingredient -> value
                is java.util.Optional<*> -> value.orElse(null) as? Ingredient
                else -> null
            }
        }
}