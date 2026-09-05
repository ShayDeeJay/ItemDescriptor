package org.shaydee.item_descriptor.screen

import net.minecraft.commands.arguments.ResourceLocationArgument.getRecipe
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import org.joml.Quaternionf
import org.shaydee.item_descriptor.Helpers.prefixComponent
import org.shaydee.item_descriptor.Helpers.toComponent
import org.shaydee.shaydeeapi.client.Icons
import org.shaydee.shaydeeapi.helpers.TextHelpers.capsFirst

enum class ButtonTypes(
    val displayName: ResourceLocation,
    val condition: (ItemDescriptorScreen) -> Boolean,
    val details: (ItemDescriptorScreen) -> Component,
    val onClick: (ItemDescriptorScreen) -> Unit
) {

    RESET(
        Icons.REFRESH.icon(),
        { true },
        { "reset".prefixComponent() },
        {
            it.itemRotation = Quaternionf()
            it.targetRotation = Quaternionf()
            it.panX = 0.0
            it.panY = 0.0
            it.targetPanX = it.panX
            it.targetPanY = it.panY
            it.zoomX = 0.0
            it.smoothToX = it.zoomX
        }
    ),

    VIEW(
        Icons.VIEW.icon(),
        { true },
        {
            val context = it.displayContext.name.lowercase().replace("_", " ").capsFirst().toComponent()
            "view".prefixComponent(isHeader = true, component = context)
        },
        {
            val entries = ItemDisplayContext.entries
            val indexOf = entries.indexOf(it.displayContext)
            val index = if(indexOf == entries.lastIndex) 0 else indexOf+1
            it.displayContext = entries[index]
        }
    ),

    TOOL_TIP(
        Icons.INFORMATION.icon(),
        { true },
        {
            val showTooltip = it.showTooltip.toString().capsFirst().toComponent()
            "show_tooltip".prefixComponent(isHeader = true, component = showTooltip)
        },
        { it.showTooltip = !it.showTooltip }
    ),

    RECIPE(
        Icons.HINT.icon(),
        { it.getRecipe().isNotEmpty() },
        {
            val recipeSuffix = (it.recipeType != null).toString().capsFirst().toComponent()
            "show_recipe".prefixComponent(isHeader = true, component = recipeSuffix)
        },
        { it.recipeType = if(it.recipeType != null) null else it.getRecipe().ifEmpty { null } }
    )

}