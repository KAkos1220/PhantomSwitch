package net.kakos1220.nomorephantoms.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.kakos1220.nomorephantoms.Nomorephantoms;
import net.kakos1220.nomorephantoms.block.custom.PhantomDisabler;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block phantom_disabler = registerBlock("phantom_disabler",
            new PhantomDisabler(AbstractBlock.Settings.create().mapColor(DyeColor.GRAY).strength(3.0f,6.0F)
                    .requiresTool().sounds(BlockSoundGroup.STONE)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(Nomorephantoms.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(Nomorephantoms.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {
        Nomorephantoms.LOGGER.info("Registering Mod Blocks for " + Nomorephantoms.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.add(ModBlocks.phantom_disabler);
        });
    }
}