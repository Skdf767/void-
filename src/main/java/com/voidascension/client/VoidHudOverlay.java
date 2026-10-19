package com.voidascension.client;

import com.voidascension.ability.AbilityDefinition;
import com.voidascension.ability.AbilityService;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.companion.CompanionAbility;
import com.voidascension.companion.CompanionData;
import com.voidascension.companion.CompanionSpecies;
import com.voidascension.config.HudAnchor;
import com.voidascension.config.VoidConfig;
import com.voidascension.core.CoreService;
import com.voidascension.data.ClientVoidData;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.evolution.EvolutionRequirements;
import com.voidascension.evolution.EvolutionTable;
import com.voidascension.progression.ProgressionService;
import com.voidascension.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Permanent Void HUD panel (default: bottom-right). Shows branding, tier, Evolution Level + XP bar, Essence, the next
 * tier's requirements with a readiness bar, Void Dash cooldown, active Companion status and abilities, and flashing warnings.
 */
public final class VoidHudOverlay implements IGuiOverlay {
    public static final VoidHudOverlay INSTANCE = new VoidHudOverlay();

    private static final int PAD = 4, ICON = 16, LINE = 10, MIN_W = 112;
    private static final int BG = 0xC0100018, BORDER = 0xA07B2CBF, ACCENT = 0xFF9D4EDD;
    private static final int BRAND = 0xC77DFF, WHITE = 0xFFFFFF, SOFT = 0xD8C8F0, MUTED = 0xA49CB4;
    private static final int READY = 0x7FFFD4, WARN = 0xFF6B6B, BAR_BG = 0xFF2A0E3F;
    private static final int WARNING_TICKS = 60;
    static final int LIFTED_BOTTOM = 76;

    // ---- cache (client thread only) ----
    private int cachedRevision = Integer.MIN_VALUE;
    private long cachedBucket = Long.MIN_VALUE;
    private int cachedFeedbackSerial = -1;
    private long warningStart;
    @Nullable private ItemStack icon;
    private boolean dormant;
    private Component brand, tierLine, essenceLine, nextLine, reqLine, dashLine, warnLine;
    private Component compLine, compStageLine, compAbilityLine, compXpLine;
    private int reqColor, dashColor, compAbilityColor;
    private float xpFrac, evoFrac, compHpFrac, compXpFrac;
    private int panelW, panelH;

    private VoidHudOverlay() {}

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || mc.options.renderDebug) return;
        if (!VoidConfig.client(VoidConfig.HUD_ENABLED)) return;

        long now = mc.level.getGameTime();
        refresh(mc, player, now);
        if (dormant && !VoidConfig.client(VoidConfig.HUD_SHOW_DORMANT)) return;

        boolean warning = warnLine != null && now - warningStart < WARNING_TICKS && now >= warningStart;
        int h = panelH + (warning ? LINE : 0);
        int w = panelW;

        // ---- position ----
        HudAnchor anchor = VoidConfig.client(VoidConfig.HUD_ANCHOR);
        int ox = VoidConfig.client(VoidConfig.HUD_OFFSET_X), oy = VoidConfig.client(VoidConfig.HUD_OFFSET_Y);
        int x0 = anchor.right ? screenWidth - w - ox : ox;
        int y0 = anchor.bottom ? screenHeight - h - oy : oy;
        if (anchor.bottom) {
            int cx = screenWidth / 2, left = cx - 91 - 30, right = cx + 91 + 30;
            if (x0 < right && x0 + w > left) y0 = Math.min(y0, screenHeight - LIFTED_BOTTOM - h);
        }
        x0 = Math.max(0, Math.min(x0, screenWidth - w));
        y0 = Math.max(0, Math.min(y0, screenHeight - h));

        // ---- panel ----
        g.fill(x0, y0, x0 + w, y0 + h, BG);
        boolean flashOn = warning && ((now - warningStart) / 5) % 2 == 0;
        g.renderOutline(x0, y0, w, h, flashOn ? 0xFFFF5555 : BORDER);
        g.fill(x0, y0 + 1, x0 + 2, y0 + h - 1, flashOn ? 0xFFFF5555 : ACCENT);

        Font font = mc.font;
        int x = x0 + PAD + 1, y = y0 + PAD;
        if (icon != null) g.renderItem(icon, x, y);
        int tx = x + ICON + 3;
        g.drawString(font, brand, tx, y, BRAND, true);
        g.drawString(font, tierLine, tx, y + LINE - 1, WHITE, true);
        y += ICON + 3;
        if (dormant) return;

        int innerW = w - 2 * PAD - 1;
        bar(g, x, y, innerW, 2, xpFrac, 0xFFC77DFF);
        y += 5;
        g.drawString(font, essenceLine, x, y, SOFT, true);
        y += LINE;
        g.drawString(font, nextLine, x, y, MUTED, true);
        y += LINE;
        if (reqLine != null) {
            g.drawString(font, reqLine, x, y, reqColor, true);
            y += LINE;
        }
        bar(g, x, y, innerW, 3, evoFrac, evoFrac >= 1F ? 0xFF7FFFD4 : ACCENT);
        y += 6;
        if (dashLine != null) {
            g.drawString(font, dashLine, x, y, dashColor, true);
            y += LINE;
        }
        if (compLine != null) {
            // Species + level + stage (e.g. "✦ Abyssal Wisp · Lv 12 [Awakened]")
            g.drawString(font, compLine, x, y, 0xD0B0FF, true);
            y += LINE;
            if (compStageLine != null) {
                g.drawString(font, compStageLine, x, y, MUTED, true);
                y += LINE;
            }
            // Health bar
            bar(g, x, y, innerW, 2, compHpFrac, 0xFFFF5555);
            y += 4;
            // XP bar for companion level progress (spec 11: XP progress)
            if (compXpLine != null) {
                g.drawString(font, compXpLine, x, y, SOFT, false);
                y += LINE - 2;
                bar(g, x, y, innerW, 2, compXpFrac, 0xFF9D4EDD);
                y += 4;
            }
            if (compAbilityLine != null) {
                g.drawString(font, compAbilityLine, x, y, compAbilityColor, true);
                y += LINE;
            }
        }
        if (warning) g.drawString(font, warnLine, x, y, WARN, true);
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        g.fill(x, y, x + w, y + h, BAR_BG);
        int fw = (int) (w * Math.max(0F, Math.min(1F, frac)));
        if (fw > 0) g.fill(x, y, x + fw, y + h, color);
    }

    // ------------------------------------------------------------------ cache

    private void refresh(Minecraft mc, Player player, long now) {
        if (icon == null) icon = new ItemStack(ModItems.VOID_CORE.get());
        VoidPlayerData d = ClientVoidData.get();

        ClientVoidData.Feedback fb = ClientVoidData.feedback();
        if (fb != null && fb.serial() != cachedFeedbackSerial) {
            cachedFeedbackSerial = fb.serial();
            warningStart = now;
            warnLine = warning(fb);
            cachedBucket = Long.MIN_VALUE;
        }

        long dashCd = d.isInitialized() ? AbilityService.cooldownRemaining(d, VoidAbilities.VOID_DASH.id(), now) : 0;
        long bucket = dashCd > 0 ? now / 2 : now / 10;
        if (ClientVoidData.revision() == cachedRevision && bucket == cachedBucket) return;
        cachedRevision = ClientVoidData.revision();
        cachedBucket = bucket;

        brand = Component.translatable("hud.void_ascension.brand");
        Font font = mc.font;
        dormant = !d.isInitialized();
        if (dormant) {
            tierLine = Component.translatable("hud.void_ascension.dormant");
            panelW = Math.max(MIN_W, ICON + 3 + Math.max(font.width(brand), font.width(tierLine)) + 2 * PAD + 1);
            panelH = ICON + 2 * PAD + 3;
            return;
        }

        tierLine = Component.translatable("hud.void_ascension.tier_level", d.getTier().displayName(), d.getEvolutionLevel());
        xpFrac = levelProgress(d);
        essenceLine = Component.translatable("hud.void_ascension.essence", d.getEssence());

        EvolutionRequirements.Check c = clientCheck();
        if (c.next() == null) {
            nextLine = Component.translatable("hud.void_ascension.max");
            reqLine = null;
            evoFrac = 1F;
        } else {
            nextLine = Component.translatable("hud.void_ascension.next", c.next().displayName());
            boolean levelOk = c.isMet(EvolutionRequirements.Requirement.EVOLUTION_LEVEL);
            boolean essenceOk = c.isMet(EvolutionRequirements.Requirement.VOID_ESSENCE);
            if (c.eligible()) {
                reqLine = Component.translatable("hud.void_ascension.ready", VoidKeyMappings.OPEN_PROGRESSION.getTranslatedKeyMessage());
                reqColor = READY;
            } else if (levelOk && essenceOk) {
                reqLine = Component.translatable("hud.void_ascension.need_core");
                reqColor = 0xFFD36B;
            } else {
                reqLine = Component.translatable("hud.void_ascension.requirements",
                        c.level(), c.requiredLevel(), c.essence(), c.requiredEssence());
                reqColor = essenceOk ? MUTED : 0xE0A0FF;
            }
            evoFrac = readiness(c);
        }

        AbilityDefinition dash = VoidAbilities.VOID_DASH;
        if (AbilityService.isUnlocked(d, dash)) {
            int cost = AbilityService.activeImpl(dash.id()).map(a -> a.essenceCost()).orElse(0);
            if (dashCd > 0) {
                dashLine = Component.translatable("hud.void_ascension.cooldown", dash.displayName(),
                        String.format(Locale.ROOT, "%.1f", dashCd / 20.0));
                dashColor = MUTED;
            } else if (d.getEssence() < cost) {
                dashLine = Component.translatable("hud.void_ascension.ability_needs_essence", dash.displayName(), cost);
                dashColor = 0xE0A0FF;
            } else {
                dashLine = Component.translatable("hud.void_ascension.ability_ready", dash.displayName(),
                        VoidKeyMappings.VOID_DASH.getTranslatedKeyMessage());
                dashColor = READY;
            }
        } else {
            dashLine = null;
        }

        // Companion HUD – spec §11: icon/portrait, species, stage, level, health, XP, ability, cooldown, warnings
        compLine = null;
        compStageLine = null;
        compXpLine = null;
        compAbilityLine = null;
        if (VoidConfig.client(VoidConfig.HUD_COMPANION_ENABLED)) {
            CompanionSpecies sel = d.getSelectedCompanion();
            CompanionData cd = sel != null ? d.getCompanion(sel) : null;
            if (cd != null && cd.isUnlocked() && cd.isActive()) {
                // Stage-aware display: "✦ Abyssal Wisp · Lv 12" + stage line
                compLine = Component.literal("✦ ").append(cd.getSpecies().displayName())
                        .append(Component.literal(" · Lv " + cd.getLevel()).withStyle(cd.getStage().color()));
                compStageLine = Component.translatable("hud.void_ascension.companion_stage", cd.getStage().displayName(), String.format(Locale.ROOT, "%.0f/%.0f HP", cd.getHealth(), cd.getMaxHealth()));
                compHpFrac = cd.getHealth() / Math.max(1.0F, cd.getMaxHealth());

                // XP progress (spec: XP progress)
                if (cd.getLevel() < 30) {
                    long nextXp = CompanionData.xpForLevel(cd.getLevel() + 1);
                    long curXp = cd.getXp();
                    long prevXp = CompanionData.xpForLevel(cd.getLevel());
                    long need = Math.max(1, nextXp - prevXp);
                    long have = Math.max(0, curXp - prevXp);
                    compXpFrac = Math.min(1.0F, have / (float) need);
                    compXpLine = Component.literal(String.format(Locale.ROOT, "XP %d/%d", curXp, nextXp));
                } else {
                    compXpFrac = 1.0F;
                    compXpLine = Component.literal("XP MAX");
                }

                CompanionAbility ab = cd.getSelectedAbility();
                if (ab != null && cd.isAbilityUnlocked(ab)) {
                    long abCd = Math.max(0L, cd.getCooldownReadyAt(ab.id()) - now);
                    if (abCd > 0) {
                        compAbilityLine = Component.translatable("hud.void_ascension.cooldown", ab.displayName(),
                                String.format(Locale.ROOT, "%.1f", abCd / 20.0));
                        compAbilityColor = MUTED;
                    } else if (d.getEssence() < ab.essenceCost()) {
                        compAbilityLine = Component.translatable("hud.void_ascension.ability_needs_essence", ab.displayName(), ab.essenceCost());
                        compAbilityColor = 0xE0A0FF;
                    } else {
                        compAbilityLine = Component.translatable("hud.void_ascension.ability_ready", ab.displayName(),
                                VoidKeyMappings.COMPANION_ABILITY.getTranslatedKeyMessage());
                        compAbilityColor = READY;
                    }
                }
            } else if (VoidConfig.client(VoidConfig.HUD_COMPANION_ENABLED) && !d.getActiveCompanions().isEmpty()) {
                // Multi-companion compact fallback: show count if selected not active
                int active = d.getActiveCompanions().size();
                int max = d.getMaxActiveCompanions();
                compLine = Component.literal(String.format(Locale.ROOT, "Companions %d/%d active [B] to cycle", active, max));
                compStageLine = null;
                compXpLine = null;
                compHpFrac = 0;
            }
        }

        int textW = Math.max(ICON + 3 + Math.max(font.width(brand), font.width(tierLine)),
                Math.max(font.width(essenceLine), font.width(nextLine)));
        if (reqLine != null) textW = Math.max(textW, font.width(reqLine));
        if (dashLine != null) textW = Math.max(textW, font.width(dashLine));
        if (compLine != null) textW = Math.max(textW, font.width(compLine));
        if (compStageLine != null) textW = Math.max(textW, font.width(compStageLine));
        if (compXpLine != null) textW = Math.max(textW, font.width(compXpLine));
        if (compAbilityLine != null) textW = Math.max(textW, font.width(compAbilityLine));
        if (warnLine != null) textW = Math.max(textW, font.width(warnLine));

        panelW = Math.max(MIN_W, textW + 2 * PAD + 1);
        int extraCompH = 0;
        if (compLine != null) {
            extraCompH = LINE; // species + level
            if (compStageLine != null) extraCompH += LINE;
            extraCompH += 4; // health bar
            if (compXpLine != null) extraCompH += LINE + 4; // XP text + bar
            if (compAbilityLine != null) extraCompH += LINE;
        }
        panelH = PAD + ICON + 3 + 5 + LINE * 2 + (reqLine != null ? LINE : 0) + 6 + (dashLine != null ? LINE : 0) + extraCompH + PAD - 2;
    }

    private static Component warning(ClientVoidData.Feedback fb) {
        AbilityDefinition def = VoidAbilities.get(fb.abilityId());
        Component name = def != null ? def.displayName() : Component.literal(fb.abilityId());
        return switch (fb.result()) {
            case NOT_ENOUGH_ESSENCE -> Component.translatable("hud.void_ascension.warn.no_essence", fb.available(), fb.required());
            case ON_COOLDOWN -> Component.translatable("hud.void_ascension.warn.cooldown", name,
                    String.format(Locale.ROOT, "%.1f", fb.cooldownTicks() / 20.0));
            case LOCKED -> Component.translatable("hud.void_ascension.warn.locked", name);
            case NOT_INITIALIZED -> Component.translatable("hud.void_ascension.warn.dormant");
            default -> Component.translatable("hud.void_ascension.warn.denied", name);
        };
    }

    static float readiness(EvolutionRequirements.Check c) {
        float lv = c.requiredLevel() <= 0 ? 1F : Math.min(1F, c.level() / (float) c.requiredLevel());
        float es = c.requiredEssence() <= 0 ? 1F : Math.min(1F, c.essence() / (float) c.requiredEssence());
        return Math.min(lv, es);
    }

    static EvolutionRequirements.Check clientCheck() {
        VoidPlayerData d = ClientVoidData.get();
        Minecraft mc = Minecraft.getInstance();
        boolean core = !VoidConfig.get(VoidConfig.EVOLUTION_REQUIRES_CARRIED_CORE)
                || (mc.player != null && CoreService.hasActiveCore(mc.player));
        return EvolutionRequirements.evaluate(d.isInitialized(), core, d.getTier(), d.getEvolutionLevel(), d.getEssence(),
                EvolutionTable::get);
    }

    static float levelProgress(VoidPlayerData data) {
        int lvl = data.getEvolutionLevel();
        if (lvl >= ProgressionService.maxLevel()) return 1.0F;
        long lo = ProgressionService.totalXpForLevel(lvl), hi = ProgressionService.totalXpForLevel(lvl + 1);
        return hi <= lo ? 1.0F : Math.max(0F, Math.min(1F, (data.getVoidExperience() - lo) / (float) (hi - lo)));
    }
}
