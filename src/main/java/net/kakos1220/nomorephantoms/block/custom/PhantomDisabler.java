package net.kakos1220.nomorephantoms.block.custom;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
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

public class PhantomDisabler extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    private static boolean HasBeenPlaced = false;

    public PhantomDisabler(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
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

        if (state.get(ACTIVE) && !newState.isOf(this)) {
            world.getServer().getGameRules().get(GameRules.DO_INSOMNIA).set(true, world.getServer());
        }

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
        if (!world.isClient) {
            boolean currentState = state.get(ACTIVE);
            BlockState newState = state.with(ACTIVE, !currentState);
            world.setBlockState(pos, newState, 3);

            String message = !currentState ? "message.phantomsdisabled" : "message.phantomsenabled";
            player.sendMessage(Text.translatable(message), true);

            world.getServer().getGameRules().get(GameRules.DO_INSOMNIA).set(state.get(ACTIVE), world.getServer());

            world.playSound(null, pos, currentState ? SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF : SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        return ActionResult.SUCCESS;
    }
}