package com.voidascension.registry;

import com.voidascension.VoidAscension;
import com.voidascension.item.VoidCoreItem;
import com.voidascension.item.VoidFragmentItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, VoidAscension.MOD_ID);

    public static final RegistryObject<Item> VOID_FRAGMENT = ITEMS.register("void_fragment",
            () -> new VoidFragmentItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> VOID_CORE = ITEMS.register("void_core",
            () -> new VoidCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> VOID_FRAGMENT_ORE = ITEMS.register("void_fragment_ore",
            () -> new BlockItem(ModBlocks.VOID_FRAGMENT_ORE.get(), new Item.Properties()));

    private ModItems() {}
}
