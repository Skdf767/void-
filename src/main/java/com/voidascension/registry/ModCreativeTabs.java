package com.voidascension.registry;

import com.voidascension.VoidAscension;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VoidAscension.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.void_ascension"))
            .icon(() -> new ItemStack(ModItems.VOID_CORE.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.VOID_FRAGMENT.get());
                output.accept(ModItems.VOID_CORE.get());
                output.accept(ModItems.VOID_FRAGMENT_ORE.get());
            })
            .build());

    private ModCreativeTabs() {}
}
