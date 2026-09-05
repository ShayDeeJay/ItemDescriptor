package org.shaydee.item_descriptor.screen

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Quaternionf
import org.shaydee.item_descriptor.CustomButton
import org.shaydee.item_descriptor.Helpers.prefixComponent
import org.shaydee.item_descriptor.Helpers.toComponent
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
import org.shaydee.shaydeeapi.helpers.ClientHelpers.drawCenterComponent
import org.shaydee.shaydeeapi.helpers.ClientHelpers.fadeBlack
import org.shaydee.shaydeeapi.helpers.ClientHelpers.refinedTooltip
import org.shaydee.shaydeeapi.helpers.ClientHelpers.stringWithBackground
import org.shaydee.shaydeeapi.helpers.ColourHelpers
import org.shaydee.shaydeeapi.helpers.ColourHelpers.headerColour
import org.shaydee.shaydeeapi.helpers.ColourHelpers.netheriteBox
import org.shaydee.shaydeeapi.helpers.ColourHelpers.subHeaderColour
import org.shaydee.shaydeeapi.helpers.RenderHelpers.customItemRendererQ
import org.shaydee.shaydeeapi.helpers.TextHelpers
import org.shaydee.shaydeeapi.helpers.TextHelpers.spacer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
class ItemDescriptorScreen (
    val codec: ItemStack,
    private val lastScreen: Screen
) : AbstractScreen() {

    val graphics = GuiGraphics(getMc(), getMc().renderBuffers().bufferSource())
    var showTooltip: Boolean = false
    var displayContext: ItemDisplayContext = ItemDisplayContext.GUI
    var recipeType: List<RecipeHolder<*>>? = null

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
    val shiftWithY = -18
    var startX1 = (graphics.centerX() - withPadding / 2).toInt() - shiftWithX()
    var startY1 = (graphics.centerY() - withPadding / 2).toInt() - shiftWithY
    var widthOffset = withPadding.toInt() / 2
    var minX = startX1 + 2
    var minY = startY1 + 2
    var maxX = startX1 + withPadding.toInt() - 2
    var maxY = startY1 + withPadding.toInt() - 2

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
        this.renderBlurredBackground(partialTick)
        this.renderBlurredBackground(partialTick)
        this.renderBlurredBackground(partialTick)
    }

    fun smoothingAnimations() {
        panX += (targetPanX - panX) * 0.15
        panY += (targetPanY - panY) * 0.15
        itemRotation.nlerp(targetRotation, 0.15f, itemRotation)
        panText += (panTextTarget - panText) * 0.15
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

    fun sharedButton(
        posX: Int,
        posY: Int,
        icon: ResourceLocation,
        hoverComponent: Component,
        onClick: () -> Unit
    ) = CustomButton(
        pX = posX,
        pY = posY,
        defaultSize = 14,
        showHover = true,
        canPress = true,
        isSelected = false,
        colour = ColourHelpers.goldCoin,
        buttonOverlay = MultiIconType.TextureIcon(icon),
        onHover = { gui, x, y  -> gui.refinedTooltip(x, y, hoverComponent) },
    ){
        onClick()
        this.rebuildWidgets()
    }

    override fun init() {
        actualSize = 160F
        withPadding = actualSize + 10
        startX1 = (graphics.centerX() - withPadding / 2).toInt() - shiftWithX()
        startY1 = (graphics.centerY() - withPadding / 2).toInt() - shiftWithY
        widthOffset = withPadding.toInt() / 2
        minX = startX1 + 2
        minY = startY1 + 2
        maxX = startX1 + withPadding.toInt() - 2
        maxY = startY1 + withPadding.toInt() - 2

        val posX = width / 2 - if(hasValidDescription()) 185 else 101
        val posY = height / 2 - 4
        this.renderable { gui, mouseX, mouseY, partial ->
            gui.boxMaker(posX-3, posY-3, 10, 26, headerColour, ColourHelpers.boxColour)
        }

        var spacer = 0
        ButtonTypes.entries.forEach {
            if(!it.condition(this)) return@forEach
            this.addRenderableWidget(
                sharedButton(posX, posY + spacer, it.displayName, it.details(this)){
                    this.rebuildWidgets()
                    it.onClick.invoke(this)
                }
            )
            spacer += 16
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if(codec.isEmpty) return
        extraBlur(partialTick)

        smoothingAnimations()
        slideGuiStats()
        renderHeader(graphics)
        bezel(graphics)
        description(graphics, mouseX, mouseY)
        renderItem(graphics, mouseX, mouseY)
        additionalInformation(graphics, mouseX, mouseY)
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

        return true
    }

    private fun renderHeader(graphics: GuiGraphics) {
        val scale = 2f
        val pose = graphics.pose()

        pose.pushPose()
        pose.scale(scale, scale, scale)
        val y1 = graphics.centerY()/2 - shiftWithY - 70
        val x1 = graphics.centerX()/2
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
        val mc = minecraft ?: return
        val sharedY = graphics.centerY() + 138
        val others = DescriptionManager.getDescription(codec.item)?.associatedItems ?: return
        if(others.isEmpty()) return
        val associated = "associated".prefixComponent(ColourHelpers.goldCoin)

        val width = max(others.size * 12, 34)
        graphics.drawCenterComponent(associated, graphics.centerX(), sharedY - 16, 0)
        graphics.boxMaker(graphics.centerX() - width, sharedY - 22, width, 20, headerColour, ColourHelpers.boxColour)

        var spacer = 0
        val itemSize = 16
        val spacing = 20
        val totalWidth = (others.size - 1) * spacing + itemSize
        val startX = graphics.centerX() - totalWidth / 2
        others.forEachIndexed { index, it ->
            val x = startX + index * spacing
            val split = it.split(".")
            val registry = BuiltInRegistries.ITEM.get(ResourceLocation.tryBuild(split[1], split[2]))
            val item = registry.defaultInstance
            if(item.isEmpty) return@forEachIndexed

            graphics.renderItem(item, x, sharedY - 4)
            if (mouseX >= x && mouseX < x + itemSize && mouseY >= sharedY && mouseY < sharedY + itemSize) {
                graphics.renderTooltip(mc.font, item, mouseX, mouseY)
            }
            spacer += spacing
        }
    }

    private fun description(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        when(hasValidDescription()) {
            false -> return
            else -> graphics.boxMaker(startX1 + 169, startY1, widthOffset, widthOffset, 0, uiFade(), uiFade())
        }

        val x = graphics.centerX() + 4
        val y = (graphics.centerY() - this.fade).toInt() + 80
        when (val recipeType = recipeType) {
            null -> renderDescription(graphics, x, y, mouseX, mouseY, getMc())
            else -> graphics.renderRecipes(recipeType, x, y, mouseX, mouseY)
        }
    }

    fun descriptionComponents(): List<Component> {
        val itemDescription = DescriptionManager.getDescription(codec.item)?.mainItem ?: ""
        val text = TextHelpers.withStyleComponentTrans(itemDescription, subHeaderColour)
        val description = TextHelpers.multiLineComponent(text.string, headerColour, ColourHelpers.offWhite, "", 290, true).toMutableList()

        if(codec.tags.toList().isNotEmpty()) {
            if(itemDescription.isNotEmpty()) description.spacer()
            description.add(Component.literal("Tags"))
        }

        codec.tags.forEach {
            description.addAll(TextHelpers.multiLineComponent(it.location.toString(), headerColour, headerColour, "", 300, true))
            description.spacer()
        }

        return description
    }

}