package net.kakos1220.nomorephantoms;

import net.fabricmc.api.ModInitializer;
import net.kakos1220.nomorephantoms.block.ModBlocks;
import net.kakos1220.nomorephantoms.block.custom.PhantomDisabler;
import net.kakos1220.nomorephantoms.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nomorephantoms implements ModInitializer {
	public static final String MOD_ID = "nomorephantoms";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		PhantomDisabler.PhantomDisablerChunkListener.registerChunkLoadListener();
	}
}