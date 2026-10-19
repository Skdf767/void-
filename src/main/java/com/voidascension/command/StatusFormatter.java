package com.voidascension.command;

import com.voidascension.ability.AbilityDefinition;
import com.voidascension.ability.AbilityService;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.core.CoreService;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.evolution.EvolutionRequirements;
import com.voidascension.evolution.EvolutionService;
import com.voidascension.evolution.EvolutionTable;
import com.voidascension.evolution.TierDefinition;
import com.voidascension.progression.ProgressionService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds localized multi-line status reports. Requirement lines use ✔/✘ symbols, not colour alone. */
public final class StatusFormatter {

    public static List<Component> lines(ServerPlayer player) {
        VoidPlayerData data = VoidCapabilities.forPlayer(player);
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("message.void_ascension.status.header", player.getDisplayName())
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        if (!data.isInitialized()) {
            out.add(Component.translatable("message.void_ascension.not_initialized").withStyle(ChatFormatting.GRAY));
            return out;
        }
        out.add(Component.translatable("message.void_ascension.status.tier", data.getTier().displayName(), data.getTier().level()));
        long next = ProgressionService.totalXpForLevel(data.getEvolutionLevel() + 1);
        boolean maxLevel = data.getEvolutionLevel() >= ProgressionService.maxLevel();
        out.add(maxLevel
                ? Component.translatable("message.void_ascension.status.level_max", data.getEvolutionLevel(), data.getVoidExperience())
                : Component.translatable("message.void_ascension.status.level", data.getEvolutionLevel(), data.getVoidExperience(), next));
        out.add(Component.translatable("message.void_ascension.status.essence",
                Component.literal(String.valueOf(data.getEssence())).withStyle(ChatFormatting.AQUA)));
        out.add(Component.translatable(CoreService.hasActiveCore(player) ? "message.void_ascension.status.core_carried"
                : data.hasBoundCore() ? "message.void_ascension.status.core_not_carried"
                : "message.void_ascension.status.core_none").withStyle(ChatFormatting.GRAY));
        out.addAll(requirementLines(player));
        TierDefinition cur = EvolutionTable.get(data.getTier());
        out.add(Component.translatable("message.void_ascension.status.bonuses", fmt(cur.bonusHealth()),
                Math.round(cur.bonusSpeed() * 100), fmt(cur.bonusDamage())).withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable("message.void_ascension.status.bonuses_defense", fmt(cur.bonusArmor()),
                fmt(cur.bonusToughness()), Math.round(cur.bonusKnockbackResist() * 100)).withStyle(ChatFormatting.GRAY));
        long now = AbilityService.now(player);
        for (AbilityDefinition def : VoidAbilities.all()) {
            boolean unlocked = AbilityService.isUnlocked(data, def);
            MutableComponent line = Component.literal(unlocked ? "✔ " : "✘ ").append(def.displayName());
            if (!unlocked) {
                line.append(" ").append(Component.translatable("message.void_ascension.status.ability_requires",
                        def.unlockTier().displayName()));
            } else {
                long cd = AbilityService.cooldownRemaining(data, def.id(), now);
                if (cd > 0) line.append(" ").append(Component.translatable("message.void_ascension.status.ability_cooldown",
                        String.format(Locale.ROOT, "%.1f", cd / 20.0)));
            }
            out.add(line.withStyle(unlocked ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        }
        return out;
    }

    public static List<Component> requirementLines(ServerPlayer player) {
        List<Component> out = new ArrayList<>();
        EvolutionRequirements.Check c = EvolutionService.check(player);
        if (c.next() == null) {
            out.add(Component.translatable("message.void_ascension.evolve.max").withStyle(ChatFormatting.GOLD));
            return out;
        }
        out.add(Component.translatable("message.void_ascension.status.next", c.next().displayName()));
        out.add(req(c.isMet(EvolutionRequirements.Requirement.ACTIVE_CORE),
                Component.translatable("message.void_ascension.req.core")));
        out.add(req(c.isMet(EvolutionRequirements.Requirement.EVOLUTION_LEVEL),
                Component.translatable("message.void_ascension.req.level", c.level(), c.requiredLevel())));
        out.add(req(c.isMet(EvolutionRequirements.Requirement.VOID_ESSENCE),
                Component.translatable("message.void_ascension.req.essence", c.essence(), c.requiredEssence())));
        return out;
    }

    private static Component req(boolean met, Component text) {
        return Component.literal(met ? "  ✔ " : "  ✘ ").append(text)
                .withStyle(met ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    private StatusFormatter() {}
}
