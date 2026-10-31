package org.shaydee.item_descriptor.screen

import com.ibm.icu.text.PluralRules
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackLinkedSet
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.RecipeHolder
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.fml.ModList
import net.neoforged.neoforge.common.CreativeModeTabRegistry
import org.joml.Quaternionf
import org.shaydee.item_descriptor.CustomButton
import org.shaydee.item_descriptor.Helpers.prefixComponent
import org.shaydee.item_descriptor.data.DescriptionManager
import org.shaydee.item_descriptor.screen.ScreenHelper.renderDescription
import org.shaydee.item_descriptor.screen.ScreenHelper.renderRecipes
import org.shaydee.item_descriptor.screen.ScreenHelper.renderWidgets
import org.shaydee.shaydeeapi.client.AbstractScreen
import org.shaydee.shaydeeapi.client.Icons
import org.shaydee.shaydeeapi.client.MultiIconType
import org.shaydee.shaydeeapi.helpers.ClientHelpers
import org.shaydee.shaydeeapi.helpers.ClientHelpers.bezelMaker
import org.shaydee.shaydeeapi.helpers.ClientHelpers.boxMaker
import org.shaydee.shaydeeapi.helpers.ClientHelpers.centerX
import org.shaydee.shaydeeapi.helpers.ClientHelpers.centerY
import org.shaydee.shaydeeapi.helpers.ClientHelpers.customisableIcon
import org.shaydee.shaydeeapi.helpers.ClientHelpers.drawCenterComponent
import org.shaydee.shaydeeapi.helpers.ClientHelpers.fadeBlack
import org.shaydee.shaydeeapi.helpers.ClientHelpers.refinedTooltip
import org.shaydee.shaydeeapi.helpers.ClientHelpers.renderTooltipFromPos
import org.shaydee.shaydeeapi.helpers.ClientHelpers.stringWithBackground
import org.shaydee.shaydeeapi.helpers.ColourHelpers
import org.shaydee.shaydeeapi.helpers.ColourHelpers.headerColour
import org.shaydee.shaydeeapi.helpers.ColourHelpers.netheriteBox
import org.shaydee.shaydeeapi.helpers.ColourHelpers.subHeaderColour
import org.shaydee.shaydeeapi.helpers.RenderHelpers.customItemRenderer
import org.shaydee.shaydeeapi.helpers.RenderHelpers.customItemRendererQ
import org.shaydee.shaydeeapi.helpers.TextHelpers
import kotlin.collections.mutableMapOf
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
class ItemDescriptorScreen (
    val codec: ItemStack,
    val lastScreen: Screen
) : AbstractScreen() {

    val graphics = GuiGraphics(getMc(), getMc().renderBuffers().bufferSource())
    var showTooltip: Boolean = false
    var displayContext: ItemDisplayContext = ItemDisplayContext.GUI
    var recipeType: List<RecipeHolder<*>>? = null
    val associatedItems = mutableListOf<ItemStack>()

    private val startAnim: Boolean = true
    private var fade: Float = 0f

    var targetPanX = 0.0
    var targetPanY = 0.0
    var itemRotation = Quaternionf() // persistent, accumulated orientation
    var targetRotation = Quaternionf(itemRotation)
    var panTextTarget = 0.0
    var panX: Double = 0.0
    var panY: Double = 0.0
    var zoomX: Double = 0.0
    var smoothToX: Double = 0.0
    var actualSize = 160F
    var panText = 0.0
    var withPadding = actualSize + 10
    val shiftWithY = 0
    var startX1 = (graphics.centerX() - withPadding / 2).toInt() - shiftWithX()
    var startY1 = (graphics.centerY() - withPadding / 2).toInt() - shiftWithY
    var widthOffset = withPadding.toInt() / 2
    var minX = startX1 + 2
    var minY = startY1 + 2
    var maxX = startX1 + withPadding.toInt() - 2
    var maxY = startY1 + withPadding.toInt() - 2

    var scrollItems = 0.0
    var scrollItemsTarget = 0.0

    fun getMc(): Minecraft = Minecraft.getInstance()
    fun shiftWithX() = if(hasValidDescription()) 84 else 0
    private fun uiColour() = subHeaderColour
    private fun uiFade(): Int = fadeBlack(0.8f)
    private fun hasValidDescription(): Boolean = descriptionComponents().isNotEmpty() || recipeType != null
    override fun isPauseScreen(): Boolean = false

    private fun inBoundsItem(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in minX..maxX && mouseY.toInt() in minY..maxY

    private fun inBoundsText(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in minX+ 169..maxX+ 169 && mouseY.toInt() in minY..maxY

    private fun bezel(graphics: GuiGraphics) {
        val width = if (hasValidDescription()) 245 else 78
        graphics.bezelMaker(startX1 - 5, startY1 - 5, widthOffset + width, widthOffset + 78)
    }

    fun getRecipe(): List<RecipeHolder<*>> {
        val level = getMc().level ?: return emptyList()
        return level.recipeManager.recipes.filter { recipe -> recipe.value.getResultItem(level.registryAccess()).item == codec.item}
    }

    private fun itemTooltip(mouseX: Int, mouseY: Int, graphics: GuiGraphics) {
        if (this.showTooltip && this.inBoundsItem(mouseX.toDouble(), mouseY.toDouble())) {
            graphics.renderTooltip(font, codec, mouseX, mouseY)
        }
    }

    fun hasScrollableText(): Double{
        if(descriptionComponents().size <= 18) return 0.0
        val length = (descriptionComponents().size - 18) * font.lineHeight
        return (-length).toDouble()
    }

    override fun shouldCloseOnEsc(): Boolean {
        return run {
            getMc().setScreen(lastScreen)
            false
        }
    }

    private fun ItemDescriptorScreen.extraBlur(partialTick: Float) {
        this.renderBackground(graphics, 0, 0, partialTick)
        this.renderTransparentBackground(graphics)
    }

    fun smoothingAnimations() {
        panX += (targetPanX - panX) * 0.15
        panY += (targetPanY - panY) * 0.15
        itemRotation.nlerp(targetRotation, 0.15f, itemRotation)
        panText += (panTextTarget - panText) * 0.15
        scrollItems += (scrollItemsTarget - scrollItems) * 0.1
        zoomX += (smoothToX - zoomX) * 0.1
    }

    private fun slideGuiStats() {
        val maxFadeIn = 150.0f
        val minFadeIn = -40.0f
        val easeFactor = 0.15f

        if (startAnim) {
            val distanceToMax = maxFadeIn - this.fade
            val fadeAmount = distanceToMax * easeFactor
            this.fade = (this.fade + fadeAmount).coerceAtMost(maxFadeIn)
        } else {
            val distanceFromMin = this.fade - minFadeIn
            val fadeAmount = distanceFromMin * easeFactor
            this.fade = (this.fade - fadeAmount).coerceAtLeast(minFadeIn)
        }
    }

    private fun additionalInformation() {
        associatedItems.clear()
        val others = DescriptionManager.getDescription(codec.item)?.associatedItems ?: return
        if(others.isEmpty()) return

        others.forEachIndexed { index, it ->
            val split = it.split(".")
            val registry = BuiltInRegistries.ITEM.get(ResourceLocation.tryBuild(split[1], split[2]))
            val item = registry.defaultInstance
            if(item.isEmpty) return@forEachIndexed

            associatedItems.add(item)
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

    val itemLists = mutableMapOf<Pair<Component, ItemStack>, MutableSet<ItemStack>>()
    val minecraftItem: ItemStack = Items.GRASS_BLOCK.defaultInstance
    var selectedGroup: Component = Component.empty()
    var hoveredItem: ItemStack = ItemStack.EMPTY

    override fun init() {
        CreativeModeTabRegistry.getSortedCreativeModeTabs()
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

        val startX = 2
        var xSpacer = startX
        var ySpacer = 4
        var count = 0

        itemLists.forEach {
            this.addRenderableWidget(
                sharedButton(xSpacer, ySpacer, MultiIconType.ItemIcon(it.key.second.item), it.key.first, selectedGroup.string == it.key.first.string){
                    selectedGroup = it.key.first
                    scrollItems = 0.0
                    scrollItemsTarget = 0.0
                }
            )
            count++
            xSpacer += 16
            if(count % 5 == 0) {
                xSpacer = startX
                ySpacer += 10
            }
        }

        actualSize = 160F
        withPadding = actualSize + 10
        startX1 = (graphics.centerX() - withPadding / 2).toInt() - shiftWithX()
        startY1 = (graphics.centerY() - withPadding / 2).toInt() - shiftWithY
        widthOffset = withPadding.toInt() / 2
        minX = startX1 + 2
        minY = startY1 + 2
        maxX = startX1 + withPadding.toInt() - 2
        maxY = startY1 + withPadding.toInt() - 2

        additionalInformation()

        val posX = width / 2 - if(hasValidDescription()) 185 else 101
        val buttons = ButtonTypes.entries.filter { it.condition(this) }
        val adjustedSize = buttons.size * 8
        val posY = height / 2 - adjustedSize + 2

        this.renderable { gui, mouseX, mouseY, partial ->
            gui.boxMaker(posX-3, posY-3, 10, adjustedSize + 2, headerColour, ColourHelpers.boxColour)
        }

        var spacer = 0

        buttons.forEach {
            this.addRenderableWidget(
                sharedButton(posX, posY + spacer, it.displayName.invoke(this), it.details(this)){
                    this.rebuildWidgets()
                    it.onClick.invoke(this)
                }
            )
            spacer += 16
        }
    }

    fun getSelected() = itemLists.filter { it.key.first == selectedGroup }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if(codec.isEmpty) return

        extraBlur(partialTick)
        val startXBound = 30
        val startYBound = 8
        var startX = startXBound
        var startY = 24
        var itemCount = 0
        val maxItemsPerRow = 12

        if(selectedGroup != Component.empty()) {
//            graphics.boxMaker(startX - 28, startY + 14, 79, 75, fillColour = ColourHelpers.boxColour)
            getSelected().firstNotNullOf {(tabInfo, items) ->
                if(startY <= height/2) {
                    graphics.customItemRenderer(tabInfo.second, 2, startY - 3, size = 14f)
                    graphics.stringWithBackground(1f, Component.literal("   ${tabInfo.first.string}"), 2, startY, ColourHelpers.boxColour, center = false)
                }

                graphics.enableScissor(startX - 28, startY + 16, width, height/2 + 28)

                items.sortedBy { it.descriptionId }.forEach {
                    itemCount++
                    val x = startX - 28
                    val y = startY + 16 + scrollItems.toInt()
                    if(y > (height/2) + 30) return@forEach
                    if(y > 20){

                        graphics.customItemRenderer(it, x, y)
                        val itemSize = 16

                        if (mouseX >= x && mouseX < x + itemSize && mouseY >= y && mouseY < y + itemSize) {
                            this.hoveredItem = it
                        }
                    }
                    startX += 20
                    if(itemCount % maxItemsPerRow == 0) {
                        startX = startXBound
                        startY += 20
                    }
                }
                startY += 44
                startX = startXBound

                graphics.disableScissor()
            }
            if(!hoveredItem.isEmpty) graphics.renderTooltip(font, hoveredItem, mouseX, mouseY)
            hoveredItem = ItemStack.EMPTY
        }


        smoothingAnimations()
        slideGuiStats()
        renderHeader(graphics)
        bezel(graphics)
        additionalInformation(graphics, mouseX, mouseY)
        description(graphics, mouseX, mouseY)
        renderItem(graphics, mouseX, mouseY)
        itemTooltip(mouseX, mouseY, graphics)
        renderWidgets(graphics, mouseX, mouseY, partialTick)
    }

    fun descriptionScrollBar(mouseX: Int, mouseY: Int, graphics: GuiGraphics) {
        if (hasScrollableText() != 0.0) {
            val maxScroll = abs(hasScrollableText()).coerceAtLeast(1.0)
            val progress = (abs(panText) / maxScroll).coerceIn(0.0, 1.0)
            val thumbY = startY1 + 1 + (progress * (155 - 6))
            val colourBorder =
                if (inBoundsText(mouseX.toDouble(), mouseY.toDouble())) ColourHelpers.goldCoin else headerColour

            graphics.boxMaker(startX1 + 331, startY1, 3, 85, netheriteBox, ColourHelpers.boxColour)
            graphics.boxMaker(startX1 + 332, thumbY.toInt(), 2, 10, colourBorder, colourBorder)
        }
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean {
        val scaleFactor = 10f
        if(inBoundsText(mouseX, mouseY)){
            val minimumValue = hasScrollableText()
            if(minimumValue == 0.0) return false
            this.panTextTarget = (panTextTarget+(scrollY*scaleFactor)).coerceIn(minimumValue, 0.0)
        }

        if(inBoundsItem(mouseX, mouseY)) smoothToX = (smoothToX + scrollY * scaleFactor).coerceIn(-50.0, 400.0)

        val rows = ((getSelected().map { it.value }.first().size) / 12) - 6
        val sizes = rows * 20
        scrollItemsTarget  = (scrollItemsTarget+(scrollY*20f)).coerceIn(min((-sizes).toDouble() + 10, -0.1), 0.0)

        return true
    }

    private fun renderHeader(graphics: GuiGraphics) {
        val scale = 2f
        val pose = graphics.pose()

        pose.pushPose()
        pose.scale(scale, scale, scale)
        val x1 = graphics.centerX()/2 + 1
        val y1 = graphics.centerY()/2 - (shiftWithY*2) - 58
        val color = codec.hoverName.style.color?.value
        val targetColor = color ?: uiColour()
        val text = TextHelpers.withStyleComponentTrans(codec.hoverName.string, targetColor)

        graphics.stringWithBackground(1F, text, x1, y1, netheriteBox)
        pose.popPose()
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        val middleClick = ClientHelpers.isMousePressed(minecraft!!, InputConstants.MOUSE_BUTTON_MIDDLE)
        val leftClick = ClientHelpers.isMousePressed(minecraft!!, InputConstants.MOUSE_BUTTON_LEFT)

        if(inBoundsText(mouseX, mouseY)){
            val minimumValue = hasScrollableText()
            if(minimumValue == 0.0) return false
            this.panTextTarget = (panTextTarget+(dragY)).coerceIn(minimumValue, 0.0)
        }

        if (inBoundsItem(mouseX, mouseY) && middleClick) {
            val yaw = Math.toRadians(dragX).toFloat()
            val pitch = Math.toRadians(-dragY).toFloat()
            Quaternionf().rotateY(yaw * 2f).rotateX(pitch * 2f).mul(targetRotation, targetRotation)
        }

        if (inBoundsItem(mouseX, mouseY) && leftClick) { targetPanX += dragX; targetPanY += dragY }
        return true
    }

    private fun renderItem(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.enableScissor(minX, minY, maxX, maxY)

        val size = (min(fade, actualSize - 80) + zoomX).toFloat()
        val startX = graphics.centerX() - size / 2f - shiftWithX()
        val startY = graphics.centerY() - size / 2f - shiftWithY


        graphics.customItemRendererQ(
            codec,
            (startX + panX).toFloat(),
            (startY + panY).toFloat(),
            itemRotation,
            size,
            displayContext
        )

        graphics.flush()
        graphics.disableScissor()

        val color = codec.hoverName.style.color
        val targetColor = color?.value ?: headerColour

        graphics.boxMaker(startX1, startY1, widthOffset, widthOffset, 0, uiFade(), targetColor)

        val colourBorder = if (inBoundsItem(mouseX.toDouble(), mouseY.toDouble())) ColourHelpers.goldCoin else headerColour
        graphics.boxMaker(startX1, startY1, widthOffset, widthOffset, colourBorder, 0)
    }


    private fun additionalInformation(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if(associatedItems.isEmpty()) return
        val sharedY = graphics.centerY() + shiftWithY + 112
        val associated = "associated".prefixComponent(ColourHelpers.goldCoin)
        val width = max(associatedItems.size * 12, 34)
        val sharedX = graphics.centerX() + 1
        graphics.customisableIcon(Icons.ATTACHMENT.icon(), sharedX,sharedY - 24, size = 22, rotation = 180f)
        graphics.drawCenterComponent(associated, sharedX, sharedY - 16, 0)
        graphics.boxMaker(sharedX - width, sharedY - 22, width, 20, headerColour, ColourHelpers.boxColour)

        var spacer = 0
        val itemSize = 16
        val spacing = 20
        val totalWidth = (associatedItems.size - 1) * spacing + itemSize
        val startX = sharedX - totalWidth / 2
        associatedItems.forEachIndexed { index, it ->
            val x = startX + index * spacing
            graphics.renderItem(it, x, sharedY - 4)
            graphics.renderTooltipFromPos(mouseX, mouseY, x, sharedY - 4, font, it)
            spacer += spacing
        }
    }

    private fun description(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        when(hasValidDescription()) {
            false -> return
            else -> graphics.boxMaker(startX1 + 169, startY1, widthOffset-1, widthOffset, 0, uiFade(), uiFade())
        }

        val x = graphics.centerX() + 4
        val y = (graphics.centerY() - this.fade).toInt() + 81
        when (val recipeType = recipeType) {
            null -> renderDescription(graphics, x, y, mouseX, mouseY, getMc())
            else -> graphics.renderRecipes(this, recipeType, x, y, mouseX, mouseY)
        }
    }

    fun descriptionComponents(): List<Component> {
        val itemDescription = DescriptionManager.getDescription(codec.item)?.mainItem ?: ""
        val text = TextHelpers.withStyleComponentTrans(itemDescription, subHeaderColour)
        val description = TextHelpers.multiLineComponent(text.string, headerColour, ColourHelpers.offWhite, "", 280, true).toMutableList()

//        if(codec.tags.toList().isNotEmpty()) {
//            if(itemDescription.isNotEmpty()) description.spacer()
//            description.add(Component.literal("Tags"))
//        }
//
//        codec.tags.forEach {
//            description.addAll(TextHelpers.multiLineComponent(it.location.toString(), headerColour, headerColour, "", 300, true))
//            description.spacer()
//        }

        return description
    }

}