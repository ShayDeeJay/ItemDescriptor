package org.shaydee.item_descriptor.data

import com.google.gson.GsonBuilder
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Item
import java.io.File
import kotlin.collections.set
import kotlin.jvm.java

data class ItemDescriptionData(
    val descriptions: MutableMap<String, ItemPropertyData> = mutableMapOf()
)

data class ItemPropertyData(
    val mainItem: String,
    val associatedItems: List<String>
)

object DescriptionManager {
    private val configFile: File = Minecraft.getInstance().gameDirectory.toPath().resolve("config/item_descriptions.json").toFile()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    var data = ItemDescriptionData()


    fun createOrSave() {
        configFile.parentFile.mkdirs()
        configFile.writeText(gson.toJson(data))
    }

    fun load(): Boolean {
        if(!configFile.exists()) createOrSave()
        data = gson.fromJson(configFile.readText(), ItemDescriptionData::class.java)
        return true
    }

    fun getDescription(itemId: String): ItemPropertyData? = data.descriptions[itemId]

    fun getDescription(item: Item): ItemPropertyData? = data.descriptions[item.descriptionId]

    fun setDescription(itemId: String, description: String, vararg associatedItems: String) {
        data.descriptions[itemId] = ItemPropertyData(description, associatedItems.toList())
    }

    fun setDescription(item: Item, vararg associated: Item, description: () -> String) {
        data.descriptions[item.descriptionId] = ItemPropertyData(description.invoke(), associated.map { it.descriptionId })
    }

    fun removeDescription(itemId: String) {
        if (data.descriptions.remove(itemId) != null) createOrSave()
    }
}