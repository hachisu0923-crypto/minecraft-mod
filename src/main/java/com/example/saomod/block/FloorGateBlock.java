package com.example.saomod.block;

import com.example.saomod.data.FloorData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class FloorGateBlock extends Block {

    // Phase 1 は Floor 1〜5 対応。Phase 2 以降は BlockEntity 化を検討
    public static final IntegerProperty REQUIRED_FLOOR =
            IntegerProperty.create("required_floor", 1, 5);

    public FloorGateBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(REQUIRED_FLOOR, 1)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REQUIRED_FLOOR);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ServerLevel serverLevel = (ServerLevel) level;
        FloorData data = FloorData.get(serverLevel);
        int required = state.getValue(REQUIRED_FLOOR);

        if (!data.isCleared(required - 1)) {
            player.displayClientMessage(
                    Component.literal("Floor " + (required - 1) + " をクリアしてください"), true);
            return InteractionResult.FAIL;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return InteractionResult.SUCCESS;
    }
}
