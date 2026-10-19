package com.voidascension.companion;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The three distinct supernatural companion species available in VOID: ASCENSION.
 */
public enum CompanionSpecies {
    ABYSSAL_WISP("abyssal_wisp", ChatFormatting.LIGHT_PURPLE, 24.0F, "Support & Sustain"),
    RIFT_STALKER("rift_stalker", ChatFormatting.RED, 20.0F, "Offensive Hunter"),
    NULL_ORACLE("null_oracle", ChatFormatting.AQUA, 28.0F, "Tactical Defender");

    private final String id;
    private final ChatFormatting color;
    private final float baseMaxHealth;
    private final String role;

    CompanionSpecies(String id, ChatFormatting color, float baseMaxHealth, String role) {
        this.id = id;
        this.color = color;
        this.baseMaxHealth = baseMaxHealth;
        this.role = role;
    }

    public String id() { return id; }

    public ChatFormatting color() { return color; }

    public float baseMaxHealth() { return baseMaxHealth; }

    public String role() { return role; }

    public String translationKey() {
        return "companion.void_ascension." + id;
    }

    public MutableComponent displayName() {
        return Component.translatable(translationKey()).withStyle(color);
    }

    public MutableComponent roleDescription() {
        return Component.translatable("companion.void_ascension." + id + ".role").withStyle(ChatFormatting.GRAY);
    }

    public List<CompanionAbility> getAbilities() {
        return CompanionAbility.getForSpecies(this);
    }

    @Nullable
    public static CompanionSpecies byId(String id) {
        if (id == null) return null;
        for (CompanionSpecies s : values()) {
            if (s.id.equalsIgnoreCase(id) || s.name().equalsIgnoreCase(id)) return s;
        }
        return null;
    }
}
