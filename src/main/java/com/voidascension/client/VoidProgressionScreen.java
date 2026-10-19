package com.voidascension.client;

import com.voidascension.ability.AbilityDefinition;
import com.voidascension.ability.AbilityService;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.config.VoidConfig;
import com.voidascension.core.CoreService;
import com.voidascension.data.ClientVoidData;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.evolution.EvolutionRequirements;
import com.voidascension.evolution.EvolutionTable;
import com.voidascension.evolution.TierDefinition;
import com.voidascension.network.RequestEvolutionPacket;
import com.voidascension.network.VoidNetwork;
import com.voidascension.progression.ProgressionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * Progression screen (default key V). Shows the synced state; the only action is "Evolve", which sends a
 * {@link RequestEvolutionPacket}. The server re-validates everything - the button state here is advisory.
 */
public class VoidProgressionScreen extends Screen {
    private static final int W = 280, H = 224;
    private Button evolveButton;
    private int left, top;

    public VoidProgressionScreen() {
        super(Component.translatable("screen.void_ascension.progression"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        evolveButton = addRenderableWidget(Button.builder(Component.translatable("screen.void_ascension.evolve"), b -> {
            if (VoidNetwork.CHANNEL != null) VoidNetwork.CHANNEL.sendToServer(new RequestEvolutionPacket());
        }).bounds(left + 10, top + H - 28, 125, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 135, top + H - 28, 125, 20).build());
        refreshButton();
    }

    @Override
    public void tick() {
        refreshButton(); // data may change through sync packets while the screen is open
    }

    private EvolutionRequirements.Check check() {
        return VoidHudOverlay.clientCheck();
    }

    private void refreshButton() {
        if (evolveButton == null) return;
        EvolutionRequirements.Check c = check();
        evolveButton.active = c.eligible();
        evolveButton.setTooltip(c.eligible() ? null : Tooltip.create(Component.translatable("screen.void_ascension.evolve.locked")));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(left, top, left + W, top + H, 0xE0100018);
        g.renderOutline(left, top, W, H, 0xFF7B2CBF);
        VoidPlayerData d = ClientVoidData.get();
        int x = left + 10, y = top + 8;
        g.drawCenteredString(font, title, left + W / 2, y, 0xE0AAFF);
        y += 16;

        if (!d.isInitialized()) {
            g.drawWordWrap(font, Component.translatable("message.void_ascension.not_initialized"), x, y, W - 20, 0xAAAAAA);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        g.drawString(font, Component.translatable("screen.void_ascension.tier", d.getTier().displayName()), x, y, 0xFFFFFF);
        y += 12;
        boolean max = d.getEvolutionLevel() >= ProgressionService.maxLevel();
        g.drawString(font, max ? Component.translatable("screen.void_ascension.level_max", d.getEvolutionLevel())
                : Component.translatable("screen.void_ascension.level", d.getEvolutionLevel(), d.getVoidExperience(),
                ProgressionService.totalXpForLevel(d.getEvolutionLevel() + 1)), x, y, 0xD0B0FF);
        y += 11;
        g.fill(x, y, x + W - 20, y + 4, 0xFF2A0E3F);
        g.fill(x, y, x + (int) ((W - 20) * VoidHudOverlay.levelProgress(d)), y + 4, 0xFF9D4EDD);
        y += 9;
        g.drawString(font, Component.translatable("screen.void_ascension.essence", d.getEssence()), x, y, 0xC77DFF);
        y += 12;
        TierDefinition cur = EvolutionTable.get(d.getTier());
        g.drawString(font, Component.translatable("message.void_ascension.status.bonuses", fmt(cur.bonusHealth()),
                Math.round(cur.bonusSpeed() * 100), fmt(cur.bonusDamage())), x, y, 0x9A9A9A);
        y += 10;
        g.drawString(font, Component.translatable("message.void_ascension.status.bonuses_defense", fmt(cur.bonusArmor()),
                fmt(cur.bonusToughness()), Math.round(cur.bonusKnockbackResist() * 100)), x, y, 0x9A9A9A);
        y += 15;

        EvolutionRequirements.Check c = check();
        if (c.next() == null) {
            g.drawString(font, Component.translatable("message.void_ascension.evolve.max"), x, y, 0xFFC94A);
            y += 12;
        } else {
            g.drawString(font, Component.translatable("message.void_ascension.status.next", c.next().displayName()), x, y, 0xFFFFFF);
            y += 11;
            y = req(g, x, y, c.isMet(EvolutionRequirements.Requirement.ACTIVE_CORE), Component.translatable("message.void_ascension.req.core"));
            y = req(g, x, y, c.isMet(EvolutionRequirements.Requirement.EVOLUTION_LEVEL),
                    Component.translatable("message.void_ascension.req.level", c.level(), c.requiredLevel()));
            y = req(g, x, y, c.isMet(EvolutionRequirements.Requirement.VOID_ESSENCE),
                    Component.translatable("message.void_ascension.req.essence", c.essence(), c.requiredEssence()));
        }
        y += 4;
        g.drawString(font, Component.translatable("screen.void_ascension.abilities"), x, y, 0xFFFFFF);
        y += 11;
        long now = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        for (AbilityDefinition def : VoidAbilities.all()) {
            boolean unlocked = AbilityService.isUnlocked(d, def);
            MutableComponent line = Component.literal(unlocked ? "✔ " : "✘ ").append(def.displayName());
            if (!unlocked) {
                line.append(" ").append(Component.translatable("message.void_ascension.status.ability_requires", def.unlockTier().displayName()));
            } else if (def.isActive()) {
                long cd = AbilityService.cooldownRemaining(d, def.id(), now);
                line.append(" ").append(cd > 0
                        ? Component.translatable("message.void_ascension.status.ability_cooldown", String.format(Locale.ROOT, "%.1f", cd / 20.0))
                        : Component.translatable("screen.void_ascension.ability_key", VoidKeyMappings.VOID_DASH.getTranslatedKeyMessage()));
            }
            g.drawString(font, line, x + 4, y, unlocked ? 0x7FFFD4 : 0x777777);
            if (mouseX >= x && mouseX < x + W - 20 && mouseY >= y - 1 && mouseY < y + 10) {
                g.renderTooltip(font, font.split(def.description(), 200), mouseX, mouseY);
            }
            y += 11;
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private int req(GuiGraphics g, int x, int y, boolean met, Component text) {
        g.drawString(font, Component.literal(met ? "✔ " : "✘ ").append(text), x + 4, y, met ? 0x55FF55 : 0xFF5555);
        return y + 11;
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
