package dev.ftb.mods.ftbquests.block;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.ftb.mods.ftbquests.FTBQuests;
import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;

/**
 * Registers items for FTB Quests, including questbook and block items.
 */
public class FTBQuestsItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(FTBQuests.MOD_ID, Registry.ITEM_REGISTRY);

    // Optional: Custom creative tab for grouping mod items
    public static final CreativeModeTab TAB = new CreativeModeTab("ftbquests") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.BOOK); // Replace with quest_book if needed
        }
    };

    // 🧱 Block Items
    public static final RegistrySupplier<Item> BARRIER_ITEM = ITEMS.register("barrier",
        () -> new BlockItem(FTBQuestsBlocks.BARRIER.get(), new Item.Properties().tab(TAB)));

    public static final RegistrySupplier<Item> STAGE_BARRIER_ITEM = ITEMS.register("stage_barrier",
        () -> new BlockItem(FTBQuestsBlocks.STAGE_BARRIER.get(), new Item.Properties().tab(TAB)));

    public static final RegistrySupplier<Item> DETECTOR_ITEM = ITEMS.register("detector",
        () -> new BlockItem(FTBQuestsBlocks.DETECTOR.get(), new Item.Properties().tab(TAB)));

    public static final RegistrySupplier<Item> LOOT_CRATE_OPENER_ITEM = ITEMS.register("loot_crate_opener",
        () -> new BlockItem(FTBQuestsBlocks.LOOT_CRATE_OPENER.get(), new Item.Properties().tab(TAB)));

    public static final RegistrySupplier<Item> SCREEN_1_ITEM = ITEMS.register("screen_1",
        () -> new BlockItem(FTBQuestsBlocks.TASK_SCREEN_1.get(), new Item.Properties().tab(TAB)));

    public static final RegistrySupplier<Item> SCREEN_3_ITEM = ITEMS.register("screen_3",
        () -> new BlockItem(FTBQuestsBlocks.TASK_SCREEN_3.get(), new Item.Properties().tab(TAB)));

    public static final RegistrySupplier<Item> SCREEN_5_ITEM = ITEMS.register("screen_5",
        () -> new BlockItem(FTBQuestsBlocks.TASK_SCREEN_5.get(), new Item.Properties().tab(TAB)));

    public static final RegistrySupplier<Item> SCREEN_7_ITEM = ITEMS.register("screen_7",
        () -> new BlockItem(FTBQuestsBlocks.TASK_SCREEN_7.get(), new Item.Properties().tab(TAB)));

    public static final RegistrySupplier<Item> AUX_SCREEN_ITEM = ITEMS.register("aux_task_screen",
        () -> new BlockItem(FTBQuestsBlocks.AUX_SCREEN.get(), new Item.Properties().tab(TAB)));

    // 📘 Quest Book Item
    public static final RegistrySupplier<Item> QUEST_BOOK = ITEMS.register("quest_book",
        () -> new Item(new Item.Properties().stacksTo(1).tab(TAB)));

    public static void register() {
        ITEMS.register();
    }
}