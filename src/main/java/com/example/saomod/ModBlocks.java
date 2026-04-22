package com.example.saomod;

import com.example.saomod.block.FloorGateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SaoMod.MOD_ID);

    public static final RegistryObject<FloorGateBlock> FLOOR_GATE =
            BLOCKS.register("floor_gate", () -> new FloorGateBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(50.0f, 1200.0f)
                            .requiresCorrectToolForDrops()
            ));
}
