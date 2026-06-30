package net.kakos1220.nomorephantoms.block;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.kakos1220.nomorephantoms.Nomorephantoms;
import net.kakos1220.nomorephantoms.block.custom.PhantomDisabler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static final Block phantom_disabler = registerBlock("phantom_disabler",
            new PhantomDisabler(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Nomorephantoms.MOD_ID, "phantom_disabler")))
                    .mapColor(DyeColor.GRAY).strength(3.0f,6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(Nomorephantoms.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(Nomorephantoms.MOD_ID, name),
                new BlockItem(block, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Nomorephantoms.MOD_ID, name)))
                        .useBlockDescriptionPrefix()));
    }

    public static void registerModBlocks() {
        Nomorephantoms.LOGGER.info("Registering Mod Blocks for " + Nomorephantoms.MOD_ID);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.accept(ModBlocks.phantom_disabler);
        });
    }
}