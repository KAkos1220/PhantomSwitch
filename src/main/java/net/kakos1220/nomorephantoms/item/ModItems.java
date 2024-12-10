package net.kakos1220.nomorephantoms.item;

import net.kakos1220.nomorephantoms.Nomorephantoms;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Nomorephantoms.MOD_ID, name), item);
    }

    public static void registerModItems() {
        Nomorephantoms.LOGGER.info("Registering Mod Items for " + Nomorephantoms.MOD_ID);
    }
}