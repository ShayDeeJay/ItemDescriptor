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
            val gunPowder = "Gunpowder".itemStyle()
            val sand = "Sand".itemStyle()
            """
                TNT is a powerful explosive block designed for those moments when subtlety simply isn’t enough. Once placed, TNT can be ignited to create a powerful explosion capable of destroying surrounding blocks and dealing damage to nearby entities.

                Crafting TNT requires 5 $gunPowder and 4 $sand, arranged in an alternating pattern within a crafting table. $gunPowder provides the explosive material, while $sand forms the casing that contains the blast until the TNT is ignited.

                TNT can be activated in several ways, including using Flint and Steel, triggering it with a redstone signal, or exposing it to another explosion. Once activated, TNT begins its fuse and will detonate shortly afterwards, so make sure you are standing a safe distance away.

                The resulting explosion can clear large areas, making TNT useful for mining, excavation, clearing structures, or simply causing completely unnecessary amounts of destruction. Be careful when using it near valuable builds, storage, machinery, or anything else you would prefer to remain intact.

                Multiple TNT blocks can also be placed together to create larger chain reactions. When one block detonates, nearby TNT may be ignited by the resulting explosion, creating a cascading series of explosions.

                TNT is a simple tool, but its power should not be underestimated. Whether you are clearing a tunnel, opening up a new area, or making questionable decisions with explosives, TNT is more than capable of getting the job done.
            """.trimIndent()
        }

        DescriptionManager.setDescription(Items.CRAFTER, Items.REDSTONE, Items.IRON_INGOT, Items.CRAFTING_TABLE, Items.DISPENSER){
            """
                Craft stuff with other stuff
            """.trimIndent()
        }
        DescriptionManager.createOrSave()
    }

}