package com.voidascension.data;

import com.voidascension.companion.CompanionData;
import com.voidascension.companion.CompanionSpecies;
import com.voidascension.evolution.VoidTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Reads and writes {@link VoidPlayerData} to NBT with validation and versioned migration.
 * <p>
 * Schema (see docs/DATA_SCHEMA.md), current {@value #CURRENT_VERSION}:
 * <pre>
 * DataVersion:int  Owner:UUID?  Initialized:byte  CoreId:UUID?  Tier:int  Essence:int  VoidXp:long
 * EvolutionLevel:int  Abilities:[string]  Cooldowns:{id:long}  VoidRescues:int
 * Companions:[{Species:string, ...}]  SelectedCompanion:string
 * </pre>
 * Version 1 (mod 0.1.0) had no DataVersion and used tiers 0-5 plus LifetimeEssence.
 * Version 2 (mod 0.2.0-alpha.1) introduced DataVersion=2.
 * Version 3 (mod 0.2.0) introduces Void Companions data.
 */
public final class PlayerDataSerializer {
    public static final int CURRENT_VERSION = 3;

    static final String K_VERSION = "DataVersion";
    static final String K_OWNER = "Owner";
    static final String K_INITIALIZED = "Initialized";
    static final String K_CORE = "CoreId";
    static final String K_TIER = "Tier";
    static final String K_ESSENCE = "Essence";
    static final String K_XP = "VoidXp";
    static final String K_LEVEL = "EvolutionLevel";
    static final String K_ABILITIES = "Abilities";
    static final String K_COOLDOWNS = "Cooldowns";
    static final String K_RESCUES = "VoidRescues";
    static final String K_V1_LIFETIME = "LifetimeEssence";
    static final String K_COMPANIONS = "Companions";
    static final String K_SELECTED_COMPANION = "SelectedCompanion";

    /**
     * Outcome of a load.
     *
     * @param sourceVersion data version found in the tag (1 for legacy, 0 for empty/new)
     * @param migrated      true if the data was upgraded from an older version
     * @param issues        human-readable list of corrected problems (empty if clean)
     */
    public record LoadReport(int sourceVersion, boolean migrated, List<String> issues) {
        public boolean clean() { return issues.isEmpty(); }
    }

    public static CompoundTag write(VoidPlayerData d) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(K_VERSION, CURRENT_VERSION);
        if (d.getOwner() != null) tag.putUUID(K_OWNER, d.getOwner());
        tag.putBoolean(K_INITIALIZED, d.isInitialized());
        if (d.getCoreId() != null) tag.putUUID(K_CORE, d.getCoreId());
        tag.putInt(K_TIER, d.getTier().level());
        tag.putInt(K_ESSENCE, d.getEssence());
        tag.putLong(K_XP, d.getVoidExperience());
        tag.putInt(K_LEVEL, d.getEvolutionLevel());
        ListTag abilities = new ListTag();
        for (String id : d.getUnlockedAbilities()) abilities.add(StringTag.valueOf(id));
        tag.put(K_ABILITIES, abilities);
        CompoundTag cds = new CompoundTag();
        d.getCooldowns().forEach(cds::putLong);
        tag.put(K_COOLDOWNS, cds);
        tag.putInt(K_RESCUES, d.getVoidRescues());

        ListTag compList = new ListTag();
        for (CompanionData cd : d.getAllCompanions().values()) {
            compList.add(cd.writeNbt());
        }
        tag.put(K_COMPANIONS, compList);
        if (d.getSelectedCompanion() != null) {
            tag.putString(K_SELECTED_COMPANION, d.getSelectedCompanion().id());
        }
        return tag;
    }

    /**
     * Loads {@code tag} into {@code into} (which is reset first).
     *
     * @param knownAbility predicate deciding which ability ids are valid; unknown ids are dropped
     */
    public static LoadReport read(CompoundTag tag, VoidPlayerData into, Predicate<String> knownAbility) {
        List<String> issues = new ArrayList<>();
        into.reset();
        into.setOwner(null);
        if (tag.isEmpty()) return new LoadReport(0, false, Collections.emptyList());

        int version = tag.contains(K_VERSION, Tag.TAG_INT) ? tag.getInt(K_VERSION) : 1;
        if (version < 1) {
            issues.add("invalid DataVersion " + version + ", treated as current");
            version = CURRENT_VERSION;
        }
        if (version == 1) {
            readV1(tag, into, issues);
            return new LoadReport(1, true, Collections.unmodifiableList(issues));
        }
        if (version > CURRENT_VERSION) {
            issues.add("DataVersion " + version + " is newer than supported " + CURRENT_VERSION
                    + " (downgraded mod?) - loading known fields only");
        }

        for (String key : new String[]{K_TIER, K_ESSENCE, K_XP, K_LEVEL, K_RESCUES}) {
            if (tag.contains(key) && !tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
                issues.add("field " + key + " has wrong type " + tag.getTagType(key) + ", reset to default");
            }
        }
        if (tag.contains(K_ABILITIES) && !tag.contains(K_ABILITIES, Tag.TAG_LIST)) issues.add("field Abilities has wrong type");
        if (tag.contains(K_COOLDOWNS) && !tag.contains(K_COOLDOWNS, Tag.TAG_COMPOUND)) issues.add("field Cooldowns has wrong type");

        if (tag.hasUUID(K_OWNER)) into.setOwner(tag.getUUID(K_OWNER));
        into.setInitialized(tag.getBoolean(K_INITIALIZED));
        if (tag.hasUUID(K_CORE)) into.setCoreId(tag.getUUID(K_CORE));

        int tierLevel = tag.getInt(K_TIER);
        VoidTier tier = VoidTier.byLevel(tierLevel);
        if (tier == null) {
            issues.add("invalid tier " + tierLevel + ", reset to HUMAN");
            tier = VoidTier.HUMAN;
        }
        into.setTier(tier);

        into.setEssence(nonNegative(tag.getInt(K_ESSENCE), "essence", issues));
        long xp = tag.getLong(K_XP);
        if (xp < 0) {
            issues.add("negative VoidXp " + xp + ", reset to 0");
            xp = 0;
        }
        into.setVoidExperience(xp);
        into.setEvolutionLevel(nonNegative(tag.getInt(K_LEVEL), "level", issues));

        Set<String> abilities = new LinkedHashSet<>();
        ListTag list = tag.getList(K_ABILITIES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (knownAbility.test(id)) abilities.add(id);
            else issues.add("unknown ability '" + id + "' removed");
        }
        into.setUnlockedAbilities(abilities);

        CompoundTag cds = tag.getCompound(K_COOLDOWNS);
        for (String id : cds.getAllKeys()) {
            if (!knownAbility.test(id)) {
                issues.add("cooldown for unknown ability '" + id + "' removed");
                continue;
            }
            long readyAt = cds.getLong(id);
            if (readyAt > 0) into.setCooldownReadyAt(id, readyAt);
        }
        into.setVoidRescues(nonNegative(tag.getInt(K_RESCUES), "rescues", issues));

        if (tag.contains(K_COMPANIONS, Tag.TAG_LIST)) {
            ListTag compList = tag.getList(K_COMPANIONS, Tag.TAG_COMPOUND);
            for (int i = 0; i < compList.size(); i++) {
                CompoundTag cTag = compList.getCompound(i);
                CompanionSpecies sp = CompanionSpecies.byId(cTag.getString("Species"));
                if (sp != null) {
                    into.getCompanion(sp).readNbt(cTag);
                }
            }
        }
        if (tag.contains(K_SELECTED_COMPANION, Tag.TAG_STRING)) {
            CompanionSpecies sel = CompanionSpecies.byId(tag.getString(K_SELECTED_COMPANION));
            if (sel != null) into.setSelectedCompanion(sel);
        }

        if (!into.isInitialized() && (into.getTier() != VoidTier.HUMAN || into.hasBoundCore())) {
            issues.add("progression present but not initialized - marked initialized");
            into.setInitialized(true);
        }
        return new LoadReport(version, version < CURRENT_VERSION, Collections.unmodifiableList(issues));
    }

    /** Legacy 0.1.0 layout: Essence, LifetimeEssence, Tier (0 Dormant .. 5 Ascendant), CoreId, VoidRescues. */
    private static void readV1(CompoundTag tag, VoidPlayerData into, List<String> issues) {
        if (tag.hasUUID(K_CORE)) into.setCoreId(tag.getUUID(K_CORE));
        into.setEssence(nonNegative(tag.getInt(K_ESSENCE), "essence", issues));
        into.setVoidExperience(Math.max(0, tag.getInt(K_V1_LIFETIME)));
        int oldTier = tag.getInt(K_TIER);
        VoidTier tier = mapV1Tier(oldTier);
        if (tier == null) {
            issues.add("invalid legacy tier " + oldTier + ", reset to HUMAN");
            tier = VoidTier.HUMAN;
        }
        into.setTier(tier);
        into.setVoidRescues(nonNegative(tag.getInt(K_RESCUES), "rescues", issues));
        into.setInitialized(into.hasBoundCore() || oldTier >= 1 || into.getEssence() > 0);
    }

    /** 0.1.0 tiers: 0 Dormant, 1 Awakened, 2 Attuned, 3 Voidtouched, 4 Voidborn, 5 Ascendant. */
    public static VoidTier mapV1Tier(int oldTier) {
        return switch (oldTier) {
            case 0, 1 -> VoidTier.HUMAN;
            case 2 -> VoidTier.ENHANCED;
            case 3 -> VoidTier.MUTANT;
            case 4 -> VoidTier.VOIDBORN;
            case 5 -> VoidTier.ASCENDED;
            default -> null;
        };
    }

    private static int nonNegative(int value, String what, List<String> issues) {
        if (value < 0) {
            issues.add("negative " + what + " " + value + ", reset to 0");
            return 0;
        }
        return value;
    }

    private PlayerDataSerializer() {}
}
