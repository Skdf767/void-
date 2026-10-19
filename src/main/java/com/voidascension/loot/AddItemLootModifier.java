package com.voidascension.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidascension.config.VoidConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Global loot modifier that appends {@code item} (random count in [min, max]) when all JSON conditions pass and a
 * roll against {@code chance + looting_bonus * looting} (times {@code loot.fragmentChanceMultiplier}, capped at 1)
 * succeeds. Disabled entirely by {@code loot.naturalFragmentSources = false}.
 */
public class AddItemLootModifier extends LootModifier {
    public static final Supplier<Codec<AddItemLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst)
                    .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item))
                    .and(Codec.INT.optionalFieldOf("min", 1).forGetter(m -> m.min))
                    .and(Codec.INT.optionalFieldOf("max", 1).forGetter(m -> m.max))
                    .and(Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(m -> m.chance))
                    .and(Codec.FLOAT.optionalFieldOf("looting_bonus", 0.0F).forGetter(m -> m.lootingBonus))
                    .apply(inst, AddItemLootModifier::new)));

    private final Item item;
    private final int min;
    private final int max;
    private final float chance;
    private final float lootingBonus;

    public AddItemLootModifier(LootItemCondition[] conditions, Item item, int min, int max, float chance, float lootingBonus) {
        super(conditions);
        this.item = item;
        this.min = Math.max(0, min);
        this.max = Math.max(this.min, max);
        this.chance = Math.max(0.0F, chance);
        this.lootingBonus = Math.max(0.0F, lootingBonus);
    }

    /** Effective drop probability for the given looting level. */
    public double effectiveChance(int looting) {
        double c = (chance + lootingBonus * Math.max(0, looting)) * VoidConfig.get(VoidConfig.FRAGMENT_CHANCE_MULTIPLIER);
        return Math.min(1.0, c);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!VoidConfig.get(VoidConfig.NATURAL_FRAGMENT_SOURCES)) return generatedLoot;
        if (context.getRandom().nextDouble() >= effectiveChance(context.getLootingModifier())) return generatedLoot;
        int count = min == max ? min : min + context.getRandom().nextInt(max - min + 1);
        if (count > 0) generatedLoot.add(new ItemStack(item, count));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
