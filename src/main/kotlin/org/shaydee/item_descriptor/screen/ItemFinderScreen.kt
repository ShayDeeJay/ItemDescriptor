package org.shaydee.item_descriptor.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackLinkedSet
import net.minecraft.world.item.Items
import net.neoforged.fml.ModList
import org.shaydee.item_descriptor.CustomButton
import org.shaydee.item_descriptor.screen.ScreenHelper.renderWidgets
import org.shaydee.shaydeeapi.client.AbstractScreen
import org.shaydee.shaydeeapi.client.MultiIconType
import org.shaydee.shaydeeapi.helpers.ClientHelpers.bezelMaker
import org.shaydee.shaydeeapi.helpers.ClientHelpers.boxMaker
import org.shaydee.shaydeeapi.helpers.ClientHelpers.refinedTooltip
import org.shaydee.shaydeeapi.helpers.ClientHelpers.stringWithBackground
import org.shaydee.shaydeeapi.helpers.ColourHelpers
import org.shaydee.shaydeeapi.helpers.RenderHelpers.customItemRenderer
import kotlin.math.min

class ItemFinderScreen (
    val codec: ItemStack,
    val lastScreen: Screen
) : AbstractScreen() {

    var scrollItems = 0.0
    var scrollItemsTarget = 0.0
    val itemLists = mutableMapOf<Pair<Component, ItemStack>, MutableSet<ItemStack>>()

    val minecraftItem: ItemStack = Items.GRASS_BLOCK.defaultInstance
    var selectedGroup: Component = Component.empty()
    var hoveredItem: ItemStack = ItemStack.EMPTY
    var editBox: EditBox? = null
    var previousText = editBox?.value

    fun smoothingAnimations() {
        scrollItems += (scrollItemsTarget - scrollItems) * 0.1
    }

    fun getForDisplay() = when(selectedGroup != Component.empty()){
        true -> getSelected().values.flatten()
        else -> getAll()
    }.filter {
        editBox?.let { text ->
            if(text.value.isNotEmpty()){
                val filterName = it.descriptionId.lowercase().contains(text.value.lowercase())
                val filterTags = it.tags.anyMatch { it.location.namespace.lowercase().contains(text.value.lowercase()) }
                val enchantments = it.get(DataComponents.STORED_ENCHANTMENTS)?.entrySet()?.map { enchantment -> enchantment.key.key }?.any { it?.location()?.path?.contains(text.value.lowercase()) == true }

                return@filter filterTags || filterName || enchantments == true
            }
        }

        return@filter true
    }

    fun getSelected() = itemLists.filter { it.key.first == selectedGroup }

    fun getAll() = itemLists.values.flatten()

    fun maxPerRow() = width/50

    fun spaceByIcons() = 20

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean {
        val rowHeight = spaceByIcons().toDouble()
        val perRow = maxPerRow().coerceAtLeast(1)
        val rows = (getForDisplay().size + perRow - 1) / perRow
        val contentHeight = rows * rowHeight
        val viewHeight = (height.toDouble()/2) - 3
        val maxScroll = (contentHeight - viewHeight).coerceAtLeast(0.0)

        scrollItemsTarget = (scrollItemsTarget + scrollY * rowHeight).coerceIn(-maxScroll, 0.0)
        return true
    }

    override fun init() {
        BuiltInRegistries.CREATIVE_MODE_TAB
            .filter { it.type == CreativeModeTab.Type.CATEGORY } // skip search/hotbar/inventory tabs
            .forEach { tab ->
                val displayItems = tab.displayItems
                displayItems.forEach { stack ->
                    val modId = BuiltInRegistries.ITEM.getKey(stack.item).namespace
                    val x = ModList.get().mods.firstOrNull { it.modId == modId }
                    val displayName = Component.literal(x?.displayName ?: return@forEach)
                    val modIcon = if (modId == "minecraft") minecraftItem else tab.iconItem
                    val modData = Pair(displayName, modIcon)
                    itemLists
                        .getOrPut(modData) { ItemStackLinkedSet.createTypeAndComponentsSet() }
                        .add(stack)
                }
            }

        val startX = width/2 - (maxPerRow() * 10) - 20
        var startY = height/2 - (itemLists.size * 6)
        var xSpacer = startX
        var count = 0

        itemLists.forEach {
            val isSelected = selectedGroup.string == it.key.first.string
            this.addRenderableWidget(
                sharedButton(xSpacer, startY, MultiIconType.ItemIcon(it.key.second.item), it.key.first, isSelected){
                    selectedGroup = if(selectedGroup == it.key.first) Component.empty() else it.key.first
                    scrollItems = 0.0
                    scrollItemsTarget = 0.0
                }
            )
            count++
            startY += 16
            if(count % 5 == 0) {
                xSpacer = startX
                startY += 10
            }
        }

        val sharedX = width / 2 - 53
        val sharedY = ((height / 4) * 3) + 14

        this.renderable { gui, mouseX, mouseY, partial ->
            gui.boxMaker(sharedX, sharedY, 55, 8, if(editBox?.isFocused == true) ColourHelpers.offWhite else ColourHelpers.headerColour, ColourHelpers.boxColour)
        }

        this.editBox = EditBox(font, sharedX + 3, sharedY + 4, 100, 14, Component.empty())

        editBox?.let{ box ->
            box.apply {
                this.isBordered = false
                this.setTextColor(ColourHelpers.offWhite)
                onClick(box.x.toDouble(), box.y.toDouble())
            }
            addRenderableWidget(box)

            this.setInitialFocus(box)
        }

    }

    fun sharedButton(
        posX: Int,
        posY: Int,
        icon: MultiIconType,
        hoverComponent: Component,
        isSelected: Boolean = false,
        onClick: () -> Unit
    ): CustomButton {
        val colour = if (icon is MultiIconType.TextureIcon) ColourHelpers.goldCoin else 1
        return CustomButton(
            pX = posX,
            pY = posY,
            defaultSize = 14,
            showHover = true,
            canPress = true,
            isSelected = isSelected,
            highlightColour = colour,
            iconColour = colour,
            buttonOverlay = icon,
            onHover = { gui, x, y  -> gui.refinedTooltip(x, y, hoverComponent) },
        ){
            onClick()
            this.rebuildWidgets()
        }
    }

    override fun render(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {

        this.renderBackground(graphics, 0, 0, partialTick)
        this.renderTransparentBackground(graphics)

        val startXBound = width/2 - (maxPerRow() * 10) + 2
        val startYBound = 8
        var startX = startXBound
        var startY = height/4
        var itemCount = 0
        val current = getSelected().map { it.key.first }.firstOrNull()
        val header = current?.string?:"All Items"

        graphics.stringWithBackground(1f, Component.literal(header), width/2, startY - 4, ColourHelpers.boxColour)

        val maxY = startY * 3
        if(editBox?.value != previousText){
            scrollItems = 0.0
            scrollItemsTarget = 0.0
            previousText = editBox?.value
        }

        graphics.boxMaker(startX-3, startY + 11, (maxPerRow()*10) + 1, startY , colourBorder = ColourHelpers.headerColour, fillColour = ColourHelpers.boxColour)
        graphics.bezelMaker(startX-7, startY + 7, (maxPerRow()*20) - 7, startY*2 - 9)
        graphics.enableScissor(0, startY + 12, width, maxY + 10)

        getForDisplay().sortedBy { it.descriptionId }.forEach {
            itemCount++
            val x = startX
            val y = startY + 14 + scrollItems.toInt()
            if(y > maxY + 4) return@forEach
            if(y > height/4){
                graphics.customItemRenderer(it, x, y)
                val itemSize = 16

                if(y < maxY){
                    if (mouseX >= x && mouseX < x + itemSize && mouseY >= y && mouseY < y + itemSize) {
                        this.hoveredItem = it
                    }
                }
            }
            startX += 20
            if(itemCount % maxPerRow() == 0) {
                startX = startXBound
                startY += spaceByIcons()
            }
        }

//        startY += 64
//        startX = startXBound

        graphics.disableScissor()

        if(!hoveredItem.isEmpty) graphics.renderTooltip(font, hoveredItem, mouseX, mouseY)
        hoveredItem = ItemStack.EMPTY

        smoothingAnimations()
        renderWidgets(graphics, mouseX, mouseY, partialTick)
    }
}