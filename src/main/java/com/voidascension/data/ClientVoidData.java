package com.voidascension.data;

import com.voidascension.ability.AbilityService;
import com.voidascension.ability.VoidAbilities;
import net.minecraft.nbt.CompoundTag;

/**
 * Client-side mirror of the local player's data, filled only by server packets. Display-only:
 * the client never makes authoritative decisions from it. Safe to class-load on a dedicated server
 * (no client-only imports).
 */
public final class ClientVoidData {
    private static final VoidPlayerData DATA = new VoidPlayerData();
    /** Incremented on every change, so the HUD can cache formatted text instead of rebuilding it every frame. */
    private static int revision;

    /** Last ability denial reported by the server (see {@code AbilityFeedbackPacket}). */
    public record Feedback(int serial, String abilityId, AbilityService.UseResult result,
                           int required, int available, int cooldownTicks) {}

    private static Feedback feedback;
    private static int feedbackSerial;

    public static VoidPlayerData get() { return DATA; }

    public static int revision() { return revision; }

    public static void apply(CompoundTag tag) {
        PlayerDataSerializer.read(tag, DATA, VoidAbilities::isKnown);
        revision++;
    }

    public static void pushFeedback(String abilityId, AbilityService.UseResult result, int required, int available,
                                    int cooldownTicks) {
        feedback = new Feedback(++feedbackSerial, abilityId, result, required, available, cooldownTicks);
    }

    /** May be null. */
    public static Feedback feedback() { return feedback; }

    public static void clear() {
        DATA.reset();
        DATA.setOwner(null);
        feedback = null;
        revision++;
    }

    private ClientVoidData() {}
}
