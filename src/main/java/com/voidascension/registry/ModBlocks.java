package com.voidascension.registry;

import com.voidascension.VoidAscension;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, VoidAscension.MOD_ID);

    /** Deep ore that generates between Y -64 and Y -32 in the Overworld. */
    public static final RegistryObject<Block> VOID_FRAGMENT_ORE = BLOCKS.register("void_fragment_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE)
                    .lightLevel(state -> 4), UniformInt.of(3, 7)));

    private ModBlocks() {}
}
