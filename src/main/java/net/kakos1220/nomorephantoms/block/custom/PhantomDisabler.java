package net.kakos1220.nomorephantoms.block.custom;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

public class PhantomDisabler extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    private static boolean HasBeenPlaced = false;

    public PhantomDisabler(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, POWERED);
    }


    private void SetPlacement(Level world, boolean IsItPlaced, BlockPos pos) {
        HasBeenPlaced = IsItPlaced;

        Path worldPath = world.getServer().getWorldPath(LevelResource.ROOT);
        Path filePath = worldPath.resolve("PhantomSwitch.json");

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("IsItPlaced", new JsonPrimitive(IsItPlaced));

        if (IsItPlaced && pos != null) {
            String positionString = pos.getX() + "," + pos.getY() + "," + pos.getZ();
            jsonObject.add("Position", new JsonPrimitive(positionString));
        } else {
            jsonObject.add("Position", new JsonPrimitive("-"));
        }

        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, jsonObject.toString());
        }

        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void Startup(Level world) {
        Path worldPath = world.getServer().getWorldPath(LevelResource.ROOT);
        Path filePath = worldPath.resolve("PhantomSwitch.json");

        if (Files.exists(filePath)) {
            try {
                String content = Files.readString(filePath);
                JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
                HasBeenPlaced = jsonObject.has("IsItPlaced") && jsonObject.get("IsItPlaced").getAsBoolean();
            }

            catch (IOException e) {
                e.printStackTrace();
                HasBeenPlaced = false;
            }
        }

        else {
            HasBeenPlaced = false;
        }
    }

    public static void IsPhantomDisablerPlaced() {
        ServerLevelEvents.LOAD.register((server, world) -> {
            if (!world.isClientSide()) {
                PhantomDisabler.Startup(world);
            }
        });
    }


    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClientSide()) {
            SetPlacement(world, true, pos);
        }

        super.onPlace(state, world, pos, oldState, notify);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel serverWorld, BlockPos pos, boolean moved) {
        if (!serverWorld.isClientSide()) {
            SetPlacement(serverWorld, false, pos);
        }

        MinecraftServer server = serverWorld.getServer();
        serverWorld.getGameRules().set(GameRules.SPAWN_PHANTOMS, true, server);

        super.affectNeighborsAfterRemoval(state, serverWorld, pos, moved);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (HasBeenPlaced) {
            Player player = ctx.getPlayer();

            if (player instanceof ServerPlayer serverPlayer
                && serverPlayer.level().getServer() != null
                && !serverPlayer.level().getServer().isSingleplayer()) {
                    serverPlayer.connection.disconnect(Component.translatable("message.blockisplaced"));
            }

            else {
                if (!ctx.getLevel().isClientSide()) {
                    ctx.getPlayer().sendOverlayMessage(Component.translatable("message.blockisplaced"));
                }

                if (ctx.getPlayer() != null) {
                    ctx.getPlayer().swing(ctx.getHand());
                    return null;
                }

                return null;
            }

            return null;
        }

        return this.defaultBlockState();
    }


    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            boolean newState = !state.getValue(ACTIVE);
            BlockState setState = state.setValue(ACTIVE, newState);
            world.setBlock(pos, setState, 3);


            ServerLevel serverWorld = (ServerLevel) world;
            MinecraftServer server = serverWorld.getServer();
            serverWorld.getGameRules().set(GameRules.SPAWN_PHANTOMS, !newState, server);

            String message = newState ? "message.phantomsenabled" : "message.phantomsdisabled";
            player.sendOverlayMessage(Component.translatable(message));

            SoundEvent sound = newState ? SoundEvents.STONE_BUTTON_CLICK_OFF : SoundEvents.STONE_BUTTON_CLICK_ON;
            world.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return InteractionResult.SUCCESS;
    }

    //Redstone Support
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return world.getBlockState(pos).getValue(ACTIVE) ? 15 : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (world instanceof ServerLevel serverWorld) {
            this.update(state, serverWorld, pos);
        }
    }

    public void update(BlockState state, ServerLevel world, BlockPos pos) {
        boolean bl = world.hasNeighborSignal(pos);
        if (bl != (Boolean)state.getValue(POWERED)) {
            BlockState blockState = state;
            if (!(Boolean)state.getValue(POWERED)) {
                blockState = state.cycle(ACTIVE);
                world.playSound(null, pos, blockState.getValue(ACTIVE) ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS);
                String message = blockState.getValue(ACTIVE) ? "message.phantomsdisabled" : "message.phantomsenabled";
                MinecraftServer server = world.getServer();
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.sendOverlayMessage(Component.translatable(message));
                }
            }

            world.setBlock(pos, blockState.setValue(POWERED, bl), Block.UPDATE_ALL);
        }
    }
}