package com.voidascension.data;

import com.voidascension.VoidAscension;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.config.VoidConfig;
import com.voidascension.evolution.TierAttributes;
import com.voidascension.evolution.VoidTier;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Capability plumbing for {@link VoidPlayerData}. */
public final class VoidCapabilities {
    public static final Capability<VoidPlayerData> VOID_DATA = CapabilityManager.get(new CapabilityToken<>() {});
    public static final net.minecraft.resources.ResourceLocation KEY = VoidAscension.id("void_data");

    /** Server data for server players; the synced mirror for the local client player. Never null. */
    public static VoidPlayerData forPlayer(Player player) {
        if (player.level().isClientSide) return ClientVoidData.get();
        return get(player).orElseGet(VoidPlayerData::new);
    }

    public static Optional<VoidPlayerData> get(Player player) {
        return player.getCapability(VOID_DATA).resolve();
    }

    /** Attached to every player. */
    public static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final VoidPlayerData data = new VoidPlayerData();
        private final LazyOptional<VoidPlayerData> optional = LazyOptional.of(() -> data);
        @Nullable private final Player player;

        public Provider() { this(null); }

        public Provider(@Nullable Player player) { this.player = player; }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return VOID_DATA.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return PlayerDataSerializer.write(data);
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            PlayerDataSerializer.LoadReport report = PlayerDataSerializer.read(tag, data, VoidAbilities::isKnown);
            if (report.migrated()) {
                VoidAscension.LOGGER.info("Migrated Void Ascension player data from version {} to {}.",
                        report.sourceVersion(), PlayerDataSerializer.CURRENT_VERSION);
            }
            if (!report.clean()) {
                VoidAscension.LOGGER.warn("Corrected Void Ascension player data (owner {}): {}", data.getOwner(), report.issues());
            } else if (VoidConfig.SERVER_SPEC.isLoaded() && VoidConfig.DEBUG_LOGGING.get()) {
                VoidAscension.LOGGER.info("Loaded Void Ascension player data for {}.", data.getOwner());
            }
            // Forge deserializes capabilities BEFORE vanilla reads "Attributes" and "Health" (Entity.load ->
            // deserializeCaps -> readAdditionalSaveData). Applying the tier modifiers here means the saved health is
            // restored against the boosted maximum instead of being clamped to 20 (0.1.0 defect D1).
            if (player != null && !player.level().isClientSide) {
                TierAttributes.apply(player, data.isInitialized() ? data.getTier() : VoidTier.HUMAN);
            }
        }
    }

    private VoidCapabilities() {}
}
