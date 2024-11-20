package net.kakos1220.nomorephantoms.block.custom;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class PhantomDisabler extends Block {
    private static final Path CONFIG_PATH = Paths.get("config/phantom_disabler.json");
    private static final String ENABLED_KEY = "phantoms_disabled";
    private static boolean phantomsDisabled = false;
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    public PhantomDisabler(Settings settings) {
        super(settings);
        loadConfig();
        this.setDefaultState(this.getStateManager().getDefaultState().with(ACTIVE, false));
    }

    public class PhantomDisablerTracker {
        public static final Set<BlockPos> PHANTOM_DISABLER_POSITIONS = new HashSet<>();
    }

    private void playClientToggleSound(PlayerEntity player, boolean state) {
        SoundEvent sound = state
                ? SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF
                : SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON;

        player.playSound(sound, 1.0f, 1.0f);
    }

    private void showClientMessage(PlayerEntity player, boolean state) {
        String message = state ? "Phantoms enabled" : "Phantoms disabled";
        player.sendMessage(Text.of(message), true);
    }

    private void updateBlockStates(World world) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        boolean newState = phantomsDisabled;

        int minChunkX = serverWorld.getServer().getPlayerManager().getViewDistance() * -1;
        int maxChunkX = serverWorld.getServer().getPlayerManager().getViewDistance();
        int minChunkZ = minChunkX;
        int maxChunkZ = maxChunkX;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = serverWorld.getChunk(chunkX, chunkZ);
                if (chunk != null) {
                    for (BlockPos pos : BlockPos.iterate(chunk.getPos().getStartX(), serverWorld.getBottomY(),
                            chunk.getPos().getStartZ(), chunk.getPos().getEndX(), serverWorld.getTopY(), chunk.getPos().getEndZ())) {

                        BlockState state = serverWorld.getBlockState(pos);

                        if (state.getBlock() instanceof PhantomDisabler) {
                            serverWorld.setBlockState(pos, state.with(PhantomDisabler.ACTIVE, newState));
                        }
                    }
                }
            }
        }
    }

    public class PhantomDisablerChunkListener {
        public static void registerChunkLoadListener() {
            ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
                if (world instanceof ServerWorld serverWorld) {
                    for (BlockPos pos : PhantomDisablerTracker.PHANTOM_DISABLER_POSITIONS) {
                        if (chunk.getPos().equals(new ChunkPos(pos))) {
                            BlockState state = serverWorld.getBlockState(pos);
                            if (state.getBlock() instanceof PhantomDisabler) {
                                serverWorld.setBlockState(pos, state.with(PhantomDisabler.ACTIVE, PhantomDisabler.phantomsDisabled));
                            }
                        }
                    }
                }
            });
        }
    }


    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        boolean newState = !state.get(ACTIVE);

        if (world.isClient) {
            world.setBlockState(pos, state.with(ACTIVE, newState));
            playClientToggleSound(player, newState);
            showClientMessage(player, newState);
            return ActionResult.SUCCESS;
        }

        world.setBlockState(pos, state.with(ACTIVE, newState));

        phantomsDisabled = newState;
        saveConfig();

        MinecraftServer server = world.getServer();
        if (server != null) {
            server.getGameRules().get(GameRules.DO_INSOMNIA).set(!phantomsDisabled, server);
        }

        updateBlockStates(world);

        return ActionResult.CONSUME;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (!world.isClient) {
            boolean initialState = PhantomDisabler.phantomsDisabled;
            world.setBlockState(pos, state.with(ACTIVE, initialState));

            PhantomDisablerTracker.PHANTOM_DISABLER_POSITIONS.add(pos);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (!world.isClient && !(newState.getBlock() instanceof PhantomDisabler)) {
            PhantomDisablerTracker.PHANTOM_DISABLER_POSITIONS.remove(pos);
        }
    }


    private static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String content = Files.readString(CONFIG_PATH);
                phantomsDisabled = Boolean.parseBoolean(content.trim());
            } else {
                phantomsDisabled = false;
                saveConfig();
            }
        } catch (Exception e) {
            System.err.println("Failed to load Phantom Disabler config: " + e.getMessage());
        }
    }

    private static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, Boolean.toString(phantomsDisabled));
        } catch (Exception e) {
            System.err.println("Failed to save Phantom Disabler config: " + e.getMessage());
        }
    }
}