package net.kakos1220.nomorephantoms.block.custom;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class PhantomDisabler extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    private static final Set<BlockPos> PHANTOM_DISABLER_POSITIONS = new HashSet<>();

    public PhantomDisabler(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(ACTIVE, false));
    }


    private static Path getWorldConfigPath(ServerWorld world) {
        Path basePath = world.getServer().getSavePath(WorldSavePath.ROOT);
        return basePath.resolve("phantom_disabler.json");
    }

    private static boolean loadWorldState(ServerWorld world) {
        try {
            Path configPath = getWorldConfigPath(world);
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                return json.get("phantomsDisabled").getAsBoolean();
            } else {
                saveWorldState(world, false);
            }
        } catch (Exception e) {
            System.err.println("Failed to load Phantom Disabler config: " + e.getMessage());
        }
        return false;
    }

    private static void saveWorldState(ServerWorld world, boolean state) {
        try {
            Path configPath = getWorldConfigPath(world);
            JsonObject json = new JsonObject();
            json.addProperty("phantomsDisabled", state);

            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, json.toString());
        } catch (Exception e) {
            System.err.println("Failed to save Phantom Disabler config: " + e.getMessage());
        }
    }


    private void playClientToggleSound(PlayerEntity player, boolean state) {
        SoundEvent sound = state ? SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON : SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF;
        player.playSound(sound, 1.0f, 1.0f);
    }

    private void showClientMessage(PlayerEntity player, boolean state) {
        String message = state ? "Phantoms Disabled!" : "Phantoms Enabled!";
        player.sendMessage(Text.of(message), true);
    }

    private void updateAllPhantomDisabler(World world, boolean state) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        for (BlockPos pos : PHANTOM_DISABLER_POSITIONS) {
            BlockState blockState = serverWorld.getBlockState(pos);
            if (blockState.getBlock() instanceof PhantomDisabler) {
                serverWorld.setBlockState(pos, blockState.with(ACTIVE, state));
            }
        }
    }

    public static void registerChunkListener() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;

            boolean currentState = loadWorldState(serverWorld);
            ChunkPos chunkPos = chunk.getPos();

            PHANTOM_DISABLER_POSITIONS.stream()
                    .filter(pos -> chunkPos.equals(new ChunkPos(pos)))
                    .forEach(pos -> {
                        BlockState blockState = serverWorld.getBlockState(pos);
                        if (blockState.getBlock() instanceof PhantomDisabler) {
                            serverWorld.setBlockState(pos, blockState.with(ACTIVE, currentState));
                        }
                    });
        });
    }


    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            boolean clientNewState = !state.get(ACTIVE);
            playClientToggleSound(player, clientNewState);
            showClientMessage(player, clientNewState);
            return ActionResult.SUCCESS;
        }

        if (!(world instanceof ServerWorld serverWorld)) return ActionResult.FAIL;

        boolean currentState = loadWorldState(serverWorld);

        boolean newState = !currentState;
        saveWorldState(serverWorld, newState);

        world.setBlockState(pos, state.with(ACTIVE, newState));

        serverWorld.getGameRules().get(GameRules.DO_INSOMNIA).set(!newState, serverWorld.getServer());

        updateAllPhantomDisabler(world, newState);

        player.sendMessage(Text.of(newState ? "Phantoms Disabled!" : "Phantoms Enabled!"), true);

        return ActionResult.SUCCESS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (!world.isClient) {
            boolean initialState = loadWorldState((ServerWorld) world);
            world.setBlockState(pos, state.with(ACTIVE, initialState));
            PHANTOM_DISABLER_POSITIONS.add(pos);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);

        if (!world.isClient && !(newState.getBlock() instanceof PhantomDisabler)) {
            PHANTOM_DISABLER_POSITIONS.remove(pos);
        }
    }
}
