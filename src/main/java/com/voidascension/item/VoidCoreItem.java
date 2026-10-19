package com.voidascension.item;

import com.voidascension.command.StatusFormatter;
import com.voidascension.config.VoidConfig;
import com.voidascension.core.CoreService;
import com.voidascension.data.ClientVoidData;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.evolution.EvolutionService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The Void Core. Crafted unbound from five Void Fragments.
 * <ul>
 *     <li><b>Unbound:</b> hold right-click to perform the activation ritual. The first activation initializes the
 *     player's progression; later activations never reset it.</li>
 *     <li><b>Active (yours):</b> right-click shows your status; sneak + right-click attempts evolution.</li>
 *     <li><b>Severed (yours, superseded):</b> inert; sneak + hold right-click to transfer the bond back (Essence penalty).</li>
 *     <li><b>Bound to someone else:</b> rejects you.</li>
 * </ul>
 * The stack stores only the bond (NBT {@value #TAG_ROOT}: Owner, OwnerName, CoreId); progression is stored on the player.
 */
public class VoidCoreItem extends Item {
    public static final String TAG_ROOT = "VoidCore";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_CORE_ID = "CoreId";

    public VoidCoreItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------ NBT helpers

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        return tag != null && tag.hasUUID(TAG_OWNER) && tag.hasUUID(TAG_CORE_ID);
    }

    @Nullable
    public static UUID getOwner(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        return tag != null && tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
    }

    @Nullable
    public static UUID getCoreId(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        return tag != null && tag.hasUUID(TAG_CORE_ID) ? tag.getUUID(TAG_CORE_ID) : null;
    }

    public static String getOwnerName(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        return tag != null ? tag.getString(TAG_OWNER_NAME) : "";
    }

    public static boolean isOwnedBy(ItemStack stack, Player player) {
        return player.getUUID().equals(getOwner(stack));
    }

    /** True if this stack is a Core bound to {@code player} AND is the player's current (non-severed) Core. */
    public static boolean isActiveFor(ItemStack stack, Player player, VoidPlayerData data) {
        if (!(stack.getItem() instanceof VoidCoreItem) || !isBound(stack) || !isOwnedBy(stack, player)) return false;
        UUID coreId = getCoreId(stack);
        return coreId != null && coreId.equals(data.getCoreId());
    }

    public static void writeBond(ItemStack stack, Player player, UUID coreId) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ROOT);
        tag.putUUID(TAG_OWNER, player.getUUID());
        tag.putString(TAG_OWNER_NAME, player.getGameProfile().getName());
        tag.putUUID(TAG_CORE_ID, coreId);
    }

    // ------------------------------------------------------------------ Use behaviour

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!isBound(stack)) {
            player.startUsingItem(hand); // activation ritual, completed in finishUsingItem
            return InteractionResultHolder.consume(stack);
        }

        if (!isOwnedBy(stack, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.void_ascension.core.rejects",
                        getOwnerName(stack)).withStyle(ChatFormatting.RED), true);
                level.playSound(null, player.blockPosition(), SoundEvents.SHULKER_HURT_CLOSED, SoundSource.PLAYERS, 1.0F, 0.5F);
            }
            return InteractionResultHolder.fail(stack);
        }

        VoidPlayerData data = VoidCapabilities.forPlayer(player);
        if (!isActiveFor(stack, player, data)) {
            if (player.isShiftKeyDown() || !data.hasBoundCore()) {
                player.startUsingItem(hand); // re-bind a severed core
                return InteractionResultHolder.consume(stack);
            }
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.void_ascension.core.severed")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (player.isShiftKeyDown()) {
                EvolutionService.tryAdvanceWithFeedback(sp);
            } else {
                StatusFormatter.lines(sp).forEach(sp::sendSystemMessage);
            }
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return VoidConfig.get(VoidConfig.BIND_DURATION_TICKS);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) {
            RandomSource r = entity.getRandom();
            for (int i = 0; i < 3; i++) {
                double angle = r.nextDouble() * Math.PI * 2;
                double radius = 1.2 + r.nextDouble() * 0.6;
                level.addParticle(ParticleTypes.PORTAL,
                        entity.getX() + Math.cos(angle) * radius, entity.getY() + r.nextDouble() * 2.0,
                        entity.getZ() + Math.sin(angle) * radius,
                        -Math.cos(angle) * 0.6, -0.2, -Math.sin(angle) * 0.6);
            }
        } else if (remainingUseDuration % 20 == 0) {
            level.playSound(null, entity.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
                    0.6F, 0.5F + (getUseDuration(stack) - remainingUseDuration) / (float) getUseDuration(stack));
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            CoreService.BindResult result = CoreService.bind(player, stack, player.isShiftKeyDown());
            switch (result) {
                case NEEDS_SNEAK_TO_TRANSFER -> {
                    player.sendSystemMessage(Component.translatable("message.void_ascension.core.needs_sneak",
                            VoidConfig.get(VoidConfig.REBIND_PENALTY_PERCENT)).withStyle(ChatFormatting.YELLOW));
                    level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(),
                            SoundSource.PLAYERS, 0.8F, 1.0F);
                }
                case OWNED_BY_OTHER -> player.displayClientMessage(Component.translatable(
                        "message.void_ascension.core.rejects", getOwnerName(stack)).withStyle(ChatFormatting.RED), true);
                default -> { }
            }
        }
        return stack;
    }

    // ------------------------------------------------------------------ Presentation

    @Override
    public boolean isFoil(ItemStack stack) {
        return isBound(stack);
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return isBound(stack) ? Integer.MAX_VALUE : super.getEntityLifespan(stack, level); // bound cores never despawn
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!isBound(stack)) {
            tooltip.add(Component.translatable("tooltip.void_ascension.core.unbound").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.void_ascension.core.unbound_hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.void_ascension.core.bound_to", getOwnerName(stack))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (level != null && level.isClientSide) {
            // ClientVoidData mirrors only the local player, so live stats are shown only on the owner's active core.
            VoidPlayerData data = ClientVoidData.get();
            UUID coreId = getCoreId(stack);
            if (coreId != null && coreId.equals(data.getCoreId())) {
                tooltip.add(Component.translatable("tooltip.void_ascension.core.tier", data.getTier().displayName())
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.void_ascension.core.level", data.getEvolutionLevel())
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.void_ascension.core.essence", data.getEssence())
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.void_ascension.core.bound_hint").withStyle(ChatFormatting.DARK_GRAY));
            } else if (data.isInitialized()) {
                tooltip.add(Component.translatable("tooltip.void_ascension.core.inert").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
