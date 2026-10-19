package com.voidascension.item;

import com.voidascension.api.event.VoidEssenceGainEvent;
import com.voidascension.config.VoidConfig;
import com.voidascension.core.CoreService;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.progression.VoidEssenceService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Void Fragment: crafting ingredient for the Void Core. Once progression is initialized (and, by default, while
 * carrying the active Core) right-click absorbs one fragment (sneak: the whole stack) into Void Essence + XP.
 */
public class VoidFragmentItem extends Item {
    public VoidFragmentItem(Properties properties) {
        super(properties);
    }

    /** Client/server-shared predicate so both sides agree on whether the use succeeds. */
    private static boolean canAbsorb(Player player) {
        VoidPlayerData data = VoidCapabilities.forPlayer(player);
        if (!data.isInitialized()) return false;
        return !VoidConfig.get(VoidConfig.ESSENCE_REQUIRES_CARRIED_CORE) || CoreService.hasActiveCore(player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return canAbsorb(player) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (!canAbsorb(sp)) {
            sp.displayClientMessage(Component.translatable(VoidCapabilities.forPlayer(sp).isInitialized()
                    ? "message.void_ascension.no_core" : "message.void_ascension.not_initialized")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.pass(stack);
        }
        int count = player.isShiftKeyDown() ? stack.getCount() : 1;
        int perFragment = VoidConfig.get(VoidConfig.FRAGMENT_ESSENCE);
        int gained = VoidEssenceService.grantFromGameplay(sp, perFragment * count, VoidEssenceGainEvent.Source.FRAGMENT);
        if (gained <= 0) return InteractionResultHolder.fail(stack);

        if (!player.getAbilities().instabuild) stack.shrink(count);
        sp.displayClientMessage(Component.translatable("message.void_ascension.absorbed", count, gained)
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        sp.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                20 + Math.min(count, 64), 0.3, 0.5, 0.3, 0.05);
        player.getCooldowns().addCooldown(this, 4);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.void_ascension.fragment").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.void_ascension.fragment_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
