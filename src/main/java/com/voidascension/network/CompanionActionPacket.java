package com.voidascension.network;

import com.voidascension.companion.CompanionAbility;
import com.voidascension.companion.CompanionData;
import com.voidascension.companion.CompanionService;
import com.voidascension.companion.CompanionSpecies;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * C2S packet: requests an action regarding a Void Companion (select, toggle active, evolve, use ability, unlock).
 * Fully validated on the server.
 */
public record CompanionActionPacket(Action action, CompanionSpecies species, int abilityIndex) {

    public enum Action {
        SELECT,
        TOGGLE_ACTIVE,
        EVOLVE,
        USE_ABILITY,
        UNLOCK
    }

    public static void encode(CompanionActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeEnum(msg.species);
        buf.writeVarInt(msg.abilityIndex);
    }

    public static CompanionActionPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        CompanionSpecies species = buf.readEnum(CompanionSpecies.class);
        int abilityIndex = buf.readVarInt();
        return new CompanionActionPacket(action, species, abilityIndex);
    }

    public static void handle(CompanionActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.isSpectator()) return;

            CompanionService.ActionResult result = switch (msg.action) {
                case SELECT -> {
                    Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
                    if (opt.isEmpty() || !opt.get().isInitialized()) {
                        yield CompanionService.ActionResult.NOT_INITIALIZED;
                    }
                    VoidPlayerData d = opt.get();
                    // Must own at least one unlocked companion to select it; verify species exists and is unlocked
                    if (d.getCompanion(msg.species) == null || !d.getCompanion(msg.species).isUnlocked()) {
                        yield CompanionService.ActionResult.LOCKED;
                    }
                    // If abilityIndex was piggybacked for ability selection, validate it
                    if (msg.abilityIndex != 0) {
                        CompanionData cd = d.getCompanion(msg.species);
                        if (cd != null && msg.abilityIndex >= 0 && msg.abilityIndex < msg.species.getAbilities().size()) {
                            CompanionAbility ab = msg.species.getAbilities().get(msg.abilityIndex);
                            if (cd.isAbilityUnlocked(ab)) {
                                cd.setSelectedAbilityIndex(msg.abilityIndex);
                            }
                        }
                    } else {
                        d.setSelectedCompanion(msg.species);
                    }
                    VoidNetwork.sync(player);
                    yield CompanionService.ActionResult.SUCCESS;
                }
                case TOGGLE_ACTIVE -> CompanionService.toggleActive(player, msg.species);
                case EVOLVE -> CompanionService.evolveCompanion(player, msg.species);
                case USE_ABILITY -> CompanionService.useAbility(player, msg.species, msg.abilityIndex);
                case UNLOCK -> CompanionService.unlockCompanion(player, msg.species);
            };

            if (!result.isSuccess()) {
                sendFailureFeedback(player, result, msg.species);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void sendFailureFeedback(ServerPlayer player, CompanionService.ActionResult result, CompanionSpecies species) {
        Component msg = switch (result) {
            case NOT_INITIALIZED -> Component.translatable("message.void_ascension.not_initialized");
            case LOCKED -> Component.translatable("message.void_ascension.companion.locked", species.displayName());
            case ALREADY_UNLOCKED -> Component.translatable("message.void_ascension.companion.already_unlocked");
            case MAX_ACTIVE_REACHED -> Component.translatable("message.void_ascension.companion.max_active");
            case RECOVERING -> Component.translatable("message.void_ascension.companion.recovering");
            case ON_COOLDOWN -> Component.translatable("message.void_ascension.companion.cooldown");
            case NOT_ENOUGH_ESSENCE -> Component.translatable("message.void_ascension.ability.no_essence", species.displayName(), "?", "?");
            case LEVEL_TOO_LOW -> Component.translatable("message.void_ascension.companion.level_too_low");
            case PLAYER_TIER_TOO_LOW -> Component.translatable("message.void_ascension.companion.player_tier_too_low");
            case MAX_STAGE_REACHED -> Component.translatable("message.void_ascension.companion.max_stage");
            case NO_TARGET -> Component.translatable("message.void_ascension.companion.no_target");
            default -> Component.translatable("message.void_ascension.companion.failed");
        };
        player.displayClientMessage(msg.copy().withStyle(ChatFormatting.RED), true);
        VoidNetwork.sendCompanionFeedback(player, new CompanionFeedbackPacket(result, species, 0));
    }
}
