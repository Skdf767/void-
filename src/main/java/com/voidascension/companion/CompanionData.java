package com.voidascension.companion;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Authoritative persistent state of a single Void Companion.
 * Stored within the player's VoidPlayerData capability.
 */
public class CompanionData {
    private final CompanionSpecies species;
    private boolean unlocked;
    private boolean active;
    private int level = 1;
    private long xp = 0;
    private CompanionStage stage = CompanionStage.DORMANT;
    private float health;
    private float maxHealth;
    private int selectedAbilityIndex = 0;
    private final Map<String, Long> cooldowns = new LinkedHashMap<>();
    private long recoveryReadyAt = 0;
    @Nullable private UUID activeEntityUUID;

    public CompanionData(CompanionSpecies species) {
        this.species = species;
        this.maxHealth = species.baseMaxHealth();
        this.health = this.maxHealth;
    }

    public CompanionSpecies getSpecies() { return species; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getLevel() { return level; }
    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(30, level));
        recalculateMaxHealth();
    }

    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = Math.max(0L, xp); }

    public CompanionStage getStage() { return stage; }
    public void setStage(CompanionStage stage) {
        this.stage = stage == null ? CompanionStage.DORMANT : stage;
        recalculateMaxHealth();
    }

    public float getHealth() { return health; }
    public void setHealth(float health) {
        this.health = Math.max(0.0F, Math.min(this.maxHealth, health));
    }

    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) {
        this.maxHealth = Math.max(1.0F, maxHealth);
        this.health = Math.min(this.health, this.maxHealth);
    }

    public void recalculateMaxHealth() {
        float bonusPerLevel = 0.5F * (level - 1);
        float stageMultiplier = switch (stage) {
            case DORMANT -> 1.0F;
            case AWAKENED -> 1.25F;
            case RESONANT -> 1.50F;
            case ASCENDANT -> 2.0F;
        };
        this.maxHealth = (species.baseMaxHealth() + bonusPerLevel) * stageMultiplier;
        if (this.health <= 0.0F && recoveryReadyAt <= 0) {
            this.health = this.maxHealth;
        } else {
            this.health = Math.min(this.health, this.maxHealth);
        }
    }

    public int getSelectedAbilityIndex() { return selectedAbilityIndex; }
    public void setSelectedAbilityIndex(int index) {
        List<CompanionAbility> abilities = species.getAbilities();
        if (index >= 0 && index < abilities.size()) {
            this.selectedAbilityIndex = index;
        }
    }

    public CompanionAbility getSelectedAbility() {
        List<CompanionAbility> abilities = species.getAbilities();
        if (selectedAbilityIndex >= 0 && selectedAbilityIndex < abilities.size()) {
            return abilities.get(selectedAbilityIndex);
        }
        return abilities.isEmpty() ? null : abilities.get(0);
    }

    public boolean isAbilityUnlocked(CompanionAbility ability) {
        return ability != null && ability.species() == this.species && this.stage.isAtLeast(ability.requiredStage());
    }

    public Map<String, Long> getCooldowns() { return Collections.unmodifiableMap(cooldowns); }

    public long getCooldownReadyAt(String abilityId) {
        return cooldowns.getOrDefault(abilityId, 0L);
    }

    public void setCooldownReadyAt(String abilityId, long readyAt) {
        if (readyAt <= 0) cooldowns.remove(abilityId);
        else cooldowns.put(abilityId, readyAt);
    }

    public void pruneCooldowns(long now) {
        cooldowns.values().removeIf(t -> t <= now);
    }

    public boolean isRecovering(long now) {
        return recoveryReadyAt > now;
    }

    /**
     * If recovery cooldown has elapsed, restore health to max and clear recovery flag.
     * @return true if recovery was completed this tick
     */
    public boolean tryCompleteRecovery(long now) {
        if (recoveryReadyAt != 0 && recoveryReadyAt <= now) {
            recoveryReadyAt = 0;
            this.health = this.maxHealth;
            return true;
        }
        return false;
    }

    public long getRecoveryReadyAt() { return recoveryReadyAt; }
    public void setRecoveryReadyAt(long recoveryReadyAt) { this.recoveryReadyAt = recoveryReadyAt; }

    @Nullable
    public UUID getActiveEntityUUID() { return activeEntityUUID; }
    public void setActiveEntityUUID(@Nullable UUID uuid) { this.activeEntityUUID = uuid; }

    // ------------------------------------------------------------------ XP Curve
    public static long xpForLevel(int targetLevel) {
        if (targetLevel <= 1) return 0L;
        double base = com.voidascension.config.VoidConfig.get(com.voidascension.config.VoidConfig.COMPANION_XP_BASE);
        double exp = com.voidascension.config.VoidConfig.get(com.voidascension.config.VoidConfig.COMPANION_XP_EXPONENT);
        return (long) Math.floor(base * Math.pow(targetLevel - 1, exp));
    }

    public static int levelForXp(long xp) {
        for (int l = 30; l >= 1; l--) {
            if (xp >= xpForLevel(l)) return l;
        }
        return 1;
    }

    public boolean addExperience(long amount) {
        if (amount <= 0 || level >= 30) return false;
        long newXp = xp + amount;
        xp = newXp < 0 ? Long.MAX_VALUE : newXp;
        int newLevel = levelForXp(xp);
        if (newLevel > level) {
            setLevel(newLevel);
            return true;
        }
        return false;
    }

    public CompoundTag writeNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Species", species.id());
        tag.putBoolean("Unlocked", unlocked);
        tag.putBoolean("Active", active);
        tag.putInt("Level", level);
        tag.putLong("Xp", xp);
        tag.putInt("Stage", stage.index());
        tag.putFloat("Health", health);
        tag.putFloat("MaxHealth", maxHealth);
        tag.putInt("SelectedAbility", selectedAbilityIndex);
        tag.putLong("RecoveryReadyAt", recoveryReadyAt);
        if (activeEntityUUID != null) tag.putUUID("ActiveEntityUUID", activeEntityUUID);

        CompoundTag cds = new CompoundTag();
        cooldowns.forEach(cds::putLong);
        tag.put("Cooldowns", cds);
        return tag;
    }

    public void readNbt(CompoundTag tag) {
        unlocked = tag.getBoolean("Unlocked");
        active = tag.getBoolean("Active");
        level = Math.max(1, Math.min(30, tag.getInt("Level")));
        xp = Math.max(0L, tag.getLong("Xp"));
        stage = CompanionStage.byIndex(tag.getInt("Stage"));
        if (stage == null) stage = CompanionStage.DORMANT;
        recalculateMaxHealth();
        if (tag.contains("Health")) {
            health = Math.max(0.0F, Math.min(maxHealth, tag.getFloat("Health")));
        } else {
            health = maxHealth;
        }
        selectedAbilityIndex = Math.max(0, Math.min(3, tag.getInt("SelectedAbility")));
        recoveryReadyAt = tag.getLong("RecoveryReadyAt");
        if (tag.hasUUID("ActiveEntityUUID")) {
            activeEntityUUID = tag.getUUID("ActiveEntityUUID");
        } else {
            activeEntityUUID = null;
        }
        cooldowns.clear();
        CompoundTag cds = tag.getCompound("Cooldowns");
        for (String key : cds.getAllKeys()) {
            cooldowns.put(key, cds.getLong(key));
        }
    }
}
