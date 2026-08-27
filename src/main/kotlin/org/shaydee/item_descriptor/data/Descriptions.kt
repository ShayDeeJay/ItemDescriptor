package org.shaydee.item_descriptor.data

import net.minecraft.ChatFormatting
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

object Descriptions {
    fun String.myFormat(vararg format: ChatFormatting): String {
        val string = format.joinToString { "§${it.char}" }.replace(", ", "")
        return "$string$this§r"
    }

    fun String.blockStyle(): String = myFormat(ChatFormatting.LIGHT_PURPLE)
    fun String.itemStyle(): String = myFormat(ChatFormatting.GOLD)


    fun initDescriptions() {
        DescriptionManager.setDescription(Blocks.TNT.asItem(), Items.SAND, Items.GUNPOWDER){
            "Blows stuff up"
        }
        DescriptionManager.createOrSave()
    }

}