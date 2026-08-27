package org.shaydee.item_descriptor

import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.common.NeoForge.EVENT_BUS
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.shaydee.item_descriptor.data.Descriptions
import org.shaydee.item_descriptor.events.ClientEvents
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

@Mod(ItemDescriptor.ID)
class ItemDescriptor {

    companion object {
        const val ID = "item_descriptor"
        val LOGGER: Logger = LogManager.getLogger(ID)
    }

    init {
        LOGGER.log(Level.INFO, "Initializing Item Descriptor")

        if (FMLEnvironment.dist == Dist.CLIENT) {
            EVENT_BUS.register(ClientEvents())
         }

        runForDist(
            clientTarget = {
                MOD_BUS.addListener(::onClientSetup)
                Minecraft.getInstance()
            }, serverTarget = {}
        )
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        Descriptions.initDescriptions()
    }

}
