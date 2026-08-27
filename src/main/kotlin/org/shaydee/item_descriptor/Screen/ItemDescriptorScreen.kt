package org.shaydee.item_descriptor.Screen

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Quaternionf
import org.shaydee.item_descriptor.CustomButton
import org.shaydee.item_descriptor.data.DescriptionManager
import org.shaydee.shaydeeapi.client.AbstractScreen
import org.shaydee.shaydeeapi.client.Icons
import org.shaydee.shaydeeapi.client.MultiIconType
import org.shaydee.shaydeeapi.helpers.ClientHelpers
import org.shaydee.shaydeeapi.helpers.ClientHelpers.bezelMaker
import org.shaydee.shaydeeapi.helpers.ClientHelpers.boxMaker
import org.shaydee.shaydeeapi.helpers.ClientHelpers.centerX
import org.shaydee.shaydeeapi.helpers.ClientHelpers.centerY
import org.shaydee.shaydeeapi.helpers.ClientHelpers.displayString
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
import kotlin.math.abs
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
class ItemDescriptorScreen (
    private val codec: ItemStack,
    private val lastScreen: Screen
) : AbstractScreen() {

    private val startAnim: Boolean = true
    private var fade: Float = 0f
    private var targetPanX = 0.0
    private var targetPanY = 0.0
    private var itemRotation = Quaternionf() // persistent, accumulated orientation
    private var targetRotation = Quaternionf(itemRotation)
    private var panText = 0.0
    private var panTextTarget = 0.0
    private var panX: Double = 0.0
    private var panY: Double = 0.0
    private var zoomX: Double = 0.0
    private var smoothToX: Double = 0.0
    private var displayContext: ItemDisplayContext = ItemDisplayContext.GUI
    private var showTooltip: Boolean = false
    private val graphics = GuiGraphics(getMc(), getMc().renderBuffers().bufferSource())

    var actualSize = 160F
    var withPadding = actualSize + 10
    var startX1 = (graphics.centerX() - withPadding / 2).toInt() - testByX()
    var startY1 = (graphics.centerY() - withPadding / 2).toInt() - testByY()
    var widthOffset = withPadding.toInt() / 2
    var minX = startX1 + 2
    var minY = startY1 + 2
    var maxX = startX1 + withPadding.toInt() - 2
    var maxY = startY1 + withPadding.toInt() - 2

    private fun uiColour() = subHeaderColour
    private fun uiFade(): Int = fadeBlack(0.8f)
    private fun getMc() = Minecraft.getInstance()
    fun testByX() = if(hasValidDescription()) 84 else 0
    fun testByY() = -18

    private fun inBoundsItem(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in minX..maxX && mouseY.toInt() in minY..maxY

    private fun inBoundsText(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in minX+ 169..maxX+ 169 && mouseY.toInt() in minY..maxY

    override fun shouldCloseOnEsc(): Boolean {
        return run {
            getMc().setScreen(lastScreen)
            false
        }
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
        defaultSize = 12,
        label = "",
        showHover = false,
        canPress = true,
        isSelected = false,
        buttonOverlay = MultiIconType.TextureIcon(icon),
        onHover = { gui, x, y  -> gui.refinedTooltip(x, y, hoverComponent) },
    ){
        onClick()
        this.rebuildWidgets()
    }

    override fun init() {
        actualSize = 160F
        withPadding = actualSize + 10
        startX1 = (graphics.centerX() - withPadding / 2).toInt() - testByX()
        startY1 = (graphics.centerY() - withPadding / 2).toInt() - testByY()
        widthOffset = withPadding.toInt() / 2
        minX = startX1 + 2
        minY = startY1 + 2
        maxX = startX1 + withPadding.toInt() - 2
        maxY = startY1 + withPadding.toInt() - 2

        val posX = width / 2 - if(hasValidDescription()) 185 else 101
        val posY = height / 2 - 4
        val spaceBy = 16
        this.renderable { gui, mouseX, mouseY, partial ->
            gui.boxMaker(posX-4, posY-4, 10, 26, headerColour, ColourHelpers.boxColour)
        }

        this.addRenderableWidget(
            sharedButton(posX, posY, Icons.REFRESH.icon(), Component.literal("Reset View")){
                itemRotation = Quaternionf()
                targetRotation = Quaternionf()
                panX = 0.0
                panY = 0.0
                targetPanX = panX
                targetPanY = panY
                zoomX = 0.0
                smoothToX = zoomX
            }
        )

        this.addRenderableWidget(
            sharedButton(posX,  posY + spaceBy, Icons.UPGRADE.icon(), Component.literal(displayContext.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() })){
                val entries = ItemDisplayContext.entries
                val indexOf = entries.indexOf(displayContext)
                val index = if(indexOf == entries.lastIndex) 0 else indexOf+1
                this.displayContext = entries[index]
            }
        )

        this.addRenderableWidget(
            sharedButton(posX,  posY + + spaceBy*2, Icons.COG.icon(), Component.literal("Show Tooltip: $showTooltip")){
                val entries = ItemDisplayContext.entries
                val indexOf = entries.indexOf(displayContext)
                val index = if(indexOf == entries.lastIndex) 0 else indexOf+1
                this.displayContext = entries[index]
            }
        )
    }

    override fun isPauseScreen(): Boolean = false

    fun smoothItem() {
        panX += (targetPanX - panX) * 0.15
        panY += (targetPanY - panY) * 0.15
        itemRotation.nlerp(targetRotation, 0.15f, itemRotation)
        panText += (panTextTarget - panText) * 0.15
        zoomX += (smoothToX - zoomX) * 0.1
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.renderBlurredBackground(partialTick)
        this.renderBlurredBackground(partialTick)
        this.renderBlurredBackground(partialTick)

        smoothItem()
        this.slideGuiStats()

        if (!codec.isEmpty) {
            renderItem(graphics, mouseX, mouseY)
            renderHeader(graphics)
            description(graphics)
            additionalInformation(graphics, mouseX, mouseY)

            if(this.showTooltip && this.inBoundsItem(mouseX.toDouble(), mouseY.toDouble())) {
                graphics.renderTooltip(font, codec, mouseX, mouseY)
            }

            if(this.hasValidDescription()){
                val maxScroll = abs(hasScrollableTest()).coerceAtLeast(1.0)
                val progress = (abs(panText) / maxScroll).coerceIn(0.0, 1.0)
                val thumbY = startY1 + 3 + (progress * (155 - 10))

                graphics.boxMaker(startX1 + 332, thumbY.toInt(), 2, 10, netheriteBox, netheriteBox)
            }
        }

        for (renderable in this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick)
        }
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean {
        println(panText)
        if(inBoundsText(mouseX, mouseY)){
            val minimumValue = hasScrollableTest()
            if(minimumValue == 0.0) return false
            this.panTextTarget = (panTextTarget+(scrollY*4)).coerceIn(minimumValue, 0.0)
        }
        if(inBoundsItem(mouseX, mouseY)) {
            val scaleFactor = 20f
            if (inBoundsItem(mouseX, mouseY)) smoothToX = (smoothToX + scrollY * scaleFactor).coerceIn(-50.0, 400.0)
        }

        return true
    }

    fun hasScrollableTest(): Double{
        if(descriptionComponents().size <= 18) return 0.0
        val length = (descriptionComponents().size - 18) * font.lineHeight
        return (-length).toDouble()
    }

    private fun renderHeader(graphics: GuiGraphics) {
        val scale = 2f
        val pose = graphics.pose()

        pose.pushPose()
        pose.scale(scale, scale, scale)
        val y1 = graphics.centerY()/2 - testByY() - 66
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
            val minimumValue = hasScrollableTest()
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
        val startX = graphics.centerX() - size / 2f - testByX()
        val startY = graphics.centerY() - size / 2f - testByY()

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
        if(hasValidDescription()) graphics.boxMaker(startX1 + 169, startY1, widthOffset, widthOffset, 0, uiFade(), uiFade())
        val width = if(hasValidDescription()) 241 else 73
        graphics.bezelMaker(startX1 - 2, startY1 -2, widthOffset + width, widthOffset + 72)
    }

    private fun additionalInformation(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val mc = minecraft ?: return
        val sharedY = graphics.centerY() + 130
        val others = DescriptionManager.getDescription(codec.item)?.associatedItems ?: return
        var spacer = 0

        if(others.isNotEmpty()) graphics.drawCenterComponent(Component.literal("Associated"), graphics.centerX(), sharedY - 16,
            ColourHelpers.goldCoin)

        others.forEach {
            val x = graphics.centerX() + spacer - (others.size*18/2)
            val i2 = 16
            val split = it.split(".")
            val registry = BuiltInRegistries.ITEM.get(ResourceLocation.tryBuild(split[1], split[2]))
            val item = registry.defaultInstance
            if(item.isEmpty) return@forEach

            graphics.renderItem(item, x, sharedY)
            spacer += 20
            if (mouseX >= x && mouseX < x + i2 && mouseY >= sharedY && mouseY < sharedY + i2) {
                graphics.renderTooltip(mc.font, item, mouseX, mouseY)
            }
        }
    }

    fun hasValidDescription(): Boolean {
        return descriptionComponents().isNotEmpty()
    }

    private fun description(graphics: GuiGraphics) {
        val x = graphics.centerX() + 4
        val y = (graphics.centerY() - this.fade).toInt() + 80
        if (!hasValidDescription()) return

        var spacer = 0
        graphics.enableScissor(minX + 169, minY, maxX + 169, maxY)

        descriptionComponents().forEach {
            graphics.displayString(it,  x, ((y - testByY() - 10 + spacer) + panText).toInt())
            spacer+=font.lineHeight
        }

        graphics.flush()
        graphics.disableScissor()

    }

    fun descriptionComponents(): List<Component> {
        val itemDescription = DescriptionManager.getDescription(codec.item)?.mainItem ?: ""
        val text = TextHelpers.withStyleComponentTrans(itemDescription, subHeaderColour)
        val description = TextHelpers.multiLineComponent(text.string, headerColour, ColourHelpers.offWhite, "", 300, true).toMutableList()

//        if(codec.tags.toList().isNotEmpty()) {
//            if(despcription.isNotEmpty()) despcription.spacer()
//            despcription.add(Component.literal("Tags"))
//        }
//
//        codec.tags.forEach {
//            despcription.addAll(TextHelpers.multiLineComponent(it.location.toString(), headerColour, ColourHelpers.headerColour, "", 300, true))
//            despcription.spacer()
//        }

        return description
    }

}