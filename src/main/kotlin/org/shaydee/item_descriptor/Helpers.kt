package org.shaydee.item_descriptor

import com.sun.tools.javac.tree.TreeInfo.args
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.shaydee.shaydeeapi.helpers.ColourHelpers.headerColour
import org.shaydee.shaydeeapi.helpers.ColourHelpers.subHeaderColour
import org.shaydee.shaydeeapi.helpers.TextHelpers
import org.shaydee.shaydeeapi.helpers.TextHelpers.displaySplitText
import org.shaydee.shaydeeapi.helpers.TextHelpers.withStyle

@OnlyIn(Dist.CLIENT)
object Helpers {

    @JvmStatic
    fun String.prefixComponent(
        colour: Int = subHeaderColour,
        bold: Boolean = false,
        underline: Boolean = false,
        strike: Boolean = false,
        isHeader: Boolean = false,
        component: Component = Component.empty(),
    ): Component = "item_descriptor.text.$this".withStyle(colour, bold, underline, strike)
        .copy()
        .append(Component.literal(if(isHeader) ": " else ""))
        .append(component)

    @JvmStatic
    @JvmOverloads
    fun displaySelectedKey(
        suffix: String,
        prefixColour: Int = headerColour,
        keyColour: Int = subHeaderColour,
    ): Component {
        val translatable = "item_descriptor.text.hold_details"
        return displaySplitText(translatable,suffix, prefixColour, keyColour)
    }


    @JvmStatic
    fun String.toComponent(colour: Int = -1): Component = TextHelpers.withStyleComponent(this, colour)

}