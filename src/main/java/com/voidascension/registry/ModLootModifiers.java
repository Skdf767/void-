package com.voidascension.registry;

import com.mojang.serialization.Codec;
import com.voidascension.VoidAscension;
import com.voidascension.loot.AddItemLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, VoidAscension.MOD_ID);

    public static final RegistryObject<Codec<AddItemLootModifier>> ADD_ITEM =
            SERIALIZERS.register("add_item", AddItemLootModifier.CODEC);

    private ModLootModifiers() {}
}
