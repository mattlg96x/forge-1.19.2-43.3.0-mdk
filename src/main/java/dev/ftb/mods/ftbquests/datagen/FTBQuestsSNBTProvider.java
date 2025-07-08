package dev.ftb.mods.ftbquests.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.DataProvider;

import java.util.concurrent.CompletableFuture;

public class FTBQuestsSNBTProvider implements DataProvider {
    private final PackOutput output;

    public FTBQuestsSNBTProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<Void> run(net.minecraft.resources.ResourceManager resourceManager) {
        // TODO: Generate SNBT quest files here (e.g. write to /data/ftbquests/quests/)
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getName() {
        return "FTB Quests SNBT Provider";
    }
}