package dev.ftb.mods.ftbquests.datagen;

import dev.ftb.mods.ftbquests.FTBQuests;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FTBQuests.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FTBQuestsDataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var existingFileHelper = event.getExistingFileHelper();

        if (event.includeServer()) {
            generator.addProvider(true, new FTBQuestsSNBTProvider(generator.getPackOutput()));
        }
    }
}