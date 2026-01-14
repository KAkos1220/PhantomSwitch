package net.kakos1220.nomorephantoms.block.custom;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

public class PhantomDisabler extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    public static final BooleanProperty POWERED = BooleanProperty.of("powered");
    private static boolean HasBeenPlaced = false;

    public PhantomDisabler(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(ACTIVE, false).with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, POWERED);
    }


    private void SetPlacement(World world, boolean IsItPlaced, BlockPos pos) {
        HasBeenPlaced = IsItPlaced;

        Path worldPath = world.getServer().getSavePath(WorldSavePath.ROOT);
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

    public static void Startup(World world) {
        Path worldPath = world.getServer().getSavePath(WorldSavePath.ROOT);
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
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!world.isClient) {
                PhantomDisabler.Startup(world);
            }
        });
    }


    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient) {
            SetPlacement(world, true, pos);
        }

        super.onBlockAdded(state, world, pos, oldState, notify);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClient && state.getBlock() != newState.getBlock()) {
            SetPlacement(world, false, pos);
        }

        world.getServer().getGameRules().get(GameRules.DO_INSOMNIA).set(true, world.getServer());

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (HasBeenPlaced) {
            PlayerEntity player = ctx.getPlayer();

            if (player instanceof ServerPlayerEntity serverPlayer && !serverPlayer.getServer().isSingleplayer()) {
                serverPlayer.networkHandler.disconnect(Text.translatable("message.blockisplaced"));
            }

            else {
                if (!ctx.getWorld().isClient) {
                    ctx.getPlayer().sendMessage(Text.translatable("message.blockisplaced"), true);
                }

                if (ctx.getPlayer() != null) {
                    ctx.getPlayer().swingHand(ctx.getHand());
                    return null;
                }

                return null;
            }

            return null;
        }

        return this.getDefaultState();
    }


    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            boolean newState = !state.get(ACTIVE);
            BlockState setState = state.with(ACTIVE, newState);
            world.setBlockState(pos, setState, 3);


            MinecraftServer server = world.getServer();
            server.getGameRules().get(GameRules.DO_INSOMNIA).set(!newState, server);

            String message = newState ? "message.phantomsenabled" : "message.phantomsdisabled";
            player.sendMessage(Text.translatable(message), true);

            SoundEvent sound = newState ? SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF : SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON;
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        return ActionResult.SUCCESS;
    }

    //Redstone Support
    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return world.getBlockState(pos).get(ACTIVE) ? 15 : 0;
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (world instanceof ServerWorld serverWorld) {
            this.update(state, serverWorld, pos);
        }
    }

    public void update(BlockState state, ServerWorld world, BlockPos pos) {
        boolean bl = world.isReceivingRedstonePower(pos);
        if (bl != (Boolean)state.get(POWERED)) {
            BlockState blockState = state;
            if (!(Boolean)state.get(POWERED)) {
                blockState = state.cycle(ACTIVE);
                world.playSound(null, pos, blockState.get(ACTIVE) ? SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON : SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS);
                String message = blockState.get(ACTIVE) ? "message.phantomsdisabled" : "message.phantomsenabled";
                MinecraftServer server = world.getServer();
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(Text.translatable(message), true);
                }
            }

            world.setBlockState(pos, blockState.with(POWERED, bl), Block.NOTIFY_ALL);
        }
    }
}