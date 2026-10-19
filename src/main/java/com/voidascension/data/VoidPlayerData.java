package com.voidascension.data;

import com.voidascension.companion.CompanionData;
import com.voidascension.companion.CompanionSpecies;
import com.voidascension.config.VoidConfig;
import com.voidascension.evolution.VoidTier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player VOID: ASCENSION state. This class only holds data and enforces basic invariants
 * (no negative values, never a null tier). Gameplay rules live in the services
 * ({@code progression}, {@code evolution}, {@code ability}, {@code companion}); serialization lives in {@link PlayerDataSerializer}.
 * <p>
 * Progression is stored on the player, never on the Void Core item, so losing the item never loses progress.
 */
public class VoidPlayerData {
    @Nullable private UUID owner;
    private boolean initialized;
    @Nullable private UUID coreId;
    private VoidTier tier = VoidTier.HUMAN;
    private int essence;
    private long voidExperience;
    private int evolutionLevel;
    private final Set<String> unlockedAbilities = new LinkedHashSet<>();
    /** ability id -> server game time (overworld) at which the ability becomes ready again. */
    private final Map<String, Long> cooldowns = new LinkedHashMap<>();
    private int voidRescues;

    // ------------------------------------------------------------------ companions
    private final Map<CompanionSpecies, CompanionData> companions = new EnumMap<>(CompanionSpecies.class);
    private CompanionSpecies selectedCompanion = CompanionSpecies.ABYSSAL_WISP;

    public VoidPlayerData() {
        for (CompanionSpecies s : CompanionSpecies.values()) {
            companions.put(s, new CompanionData(s));
        }
        // First species is unlocked by default
        companions.get(CompanionSpecies.ABYSSAL_WISP).setUnlocked(true);
    }

    // ------------------------------------------------------------------ identity / activation

    @Nullable public UUID getOwner() { return owner; }

    public void setOwner(@Nullable UUID owner) { this.owner = owner; }

    /** True once the player has activated their first Void Core. Never reset by losing or re-binding a Core. */
    public boolean isInitialized() { return initialized; }

    public void setInitialized(boolean initialized) { this.initialized = initialized; }

    /** Id of the Core the player is currently bonded to, or {@code null} (lost/unbound). */
    @Nullable public UUID getCoreId() { return coreId; }

    public void setCoreId(@Nullable UUID coreId) { this.coreId = coreId; }

    public boolean hasBoundCore() { return coreId != null; }

    // ------------------------------------------------------------------ progression

    public VoidTier getTier() { return tier; }

    public void setTier(@Nullable VoidTier tier) { this.tier = tier == null ? VoidTier.HUMAN : tier; }

    public int getEssence() { return essence; }

    public void setEssence(int essence) { this.essence = Math.max(0, essence); }

    /** Lifetime Void Experience (drives Evolution Level). Never decreases through spending. */
    public long getVoidExperience() { return voidExperience; }

    public void setVoidExperience(long xp) { this.voidExperience = Math.max(0L, xp); }

    public int getEvolutionLevel() { return evolutionLevel; }

    public void setEvolutionLevel(int level) { this.evolutionLevel = Math.max(0, level); }

    // ------------------------------------------------------------------ abilities

    public Set<String> getUnlockedAbilities() { return Collections.unmodifiableSet(unlockedAbilities); }

    public boolean isAbilityUnlocked(String id) { return unlockedAbilities.contains(id); }

    public void setUnlockedAbilities(Set<String> ids) {
        unlockedAbilities.clear();
        unlockedAbilities.addAll(ids);
    }

    public Map<String, Long> getCooldowns() { return Collections.unmodifiableMap(cooldowns); }

    public long getCooldownReadyAt(String id) { return cooldowns.getOrDefault(id, 0L); }

    public void setCooldownReadyAt(String id, long readyAt) {
        if (readyAt <= 0) cooldowns.remove(id);
        else cooldowns.put(id, readyAt);
    }

    public void clearCooldowns() { cooldowns.clear(); }

    /** Removes entries that are already expired at {@code now}. */
    public void pruneCooldowns(long now) {
        cooldowns.values().removeIf(t -> t <= now);
        for (CompanionData cd : companions.values()) {
            cd.pruneCooldowns(now);
        }
    }

    public int getVoidRescues() { return voidRescues; }

    public void setVoidRescues(int rescues) { this.voidRescues = Math.max(0, rescues); }

    // ------------------------------------------------------------------ companions

    public CompanionData getCompanion(CompanionSpecies species) {
        return companions.get(species);
    }

    public Map<CompanionSpecies, CompanionData> getAllCompanions() {
        return Collections.unmodifiableMap(companions);
    }

    public CompanionSpecies getSelectedCompanion() {
        return selectedCompanion;
    }

    public void setSelectedCompanion(CompanionSpecies selectedCompanion) {
        if (selectedCompanion != null) {
            this.selectedCompanion = selectedCompanion;
        }
    }

    public List<CompanionData> getActiveCompanions() {
        List<CompanionData> active = new ArrayList<>();
        for (CompanionData d : companions.values()) {
            if (d.isUnlocked() && d.isActive()) {
                active.add(d);
            }
        }
        return active;
    }

    public int getActiveCompanionCount() {
        int count = 0;
        for (CompanionData d : companions.values()) {
            if (d.isUnlocked() && d.isActive()) count++;
        }
        return count;
    }

    public int getMaxActiveCompanions() {
        if (tier.isAtLeast(VoidTier.ASCENDED)) {
            return VoidConfig.get(VoidConfig.COMPANIONS_MAX_ACTIVE_ASCENDED);
        } else if (tier.isAtLeast(VoidTier.MUTANT)) {
            return VoidConfig.get(VoidConfig.COMPANIONS_MAX_ACTIVE_MUTANT);
        } else {
            return VoidConfig.get(VoidConfig.COMPANIONS_MAX_ACTIVE_HUMAN);
        }
    }

    // ------------------------------------------------------------------ bulk

    /** Resets everything except the owner (used by admin reset). */
    public void reset() {
        initialized = false;
        coreId = null;
        tier = VoidTier.HUMAN;
        essence = 0;
        voidExperience = 0;
        evolutionLevel = 0;
        unlockedAbilities.clear();
        cooldowns.clear();
        voidRescues = 0;
        companions.clear();
        for (CompanionSpecies s : CompanionSpecies.values()) {
            companions.put(s, new CompanionData(s));
        }
        companions.get(CompanionSpecies.ABYSSAL_WISP).setUnlocked(true);
        selectedCompanion = CompanionSpecies.ABYSSAL_WISP;
    }

    public void copyFrom(VoidPlayerData other) {
        owner = other.owner;
        initialized = other.initialized;
        coreId = other.coreId;
        tier = other.tier;
        essence = other.essence;
        voidExperience = other.voidExperience;
        evolutionLevel = other.evolutionLevel;
        setUnlockedAbilities(other.unlockedAbilities);
        cooldowns.clear();
        cooldowns.putAll(other.cooldowns);
        voidRescues = other.voidRescues;
        this.selectedCompanion = other.selectedCompanion;
        for (CompanionSpecies s : CompanionSpecies.values()) {
            CompanionData src = other.getCompanion(s);
            if (src != null) {
                CompanionData dst = this.getCompanion(s);
                dst.readNbt(src.writeNbt());
            }
        }
    }
}
