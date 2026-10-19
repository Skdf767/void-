package com.voidascension.client.gui;

import com.voidascension.companion.CompanionAbility;
import com.voidascension.companion.CompanionData;
import com.voidascension.companion.CompanionSpecies;
import com.voidascension.companion.CompanionStage;
import com.voidascension.data.ClientVoidData;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.network.CompanionActionPacket;
import com.voidascension.network.VoidNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

/**
 * Interactive in-game Void Companion management screen.
 * Opened with keybind 'C' by default.
 */
public class VoidCompanionScreen extends Screen {

    private static final int W = 320, H = 228;
    private int left, top;

    private CompanionSpecies selectedSpecies = CompanionSpecies.ABYSSAL_WISP;

    private Button toggleActiveBtn;
    private Button evolveBtn;
    private Button unlockBtn;
    private Button setPrimaryBtn;

    public VoidCompanionScreen() {
        super(Component.translatable("screen.void_ascension.companions"));
        VoidPlayerData d = ClientVoidData.get();
        if (d.getSelectedCompanion() != null) {
            this.selectedSpecies = d.getSelectedCompanion();
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;

        // Species Tab buttons on the left
        int tabY = top + 28;
        for (CompanionSpecies sp : CompanionSpecies.values()) {
            final CompanionSpecies currentSp = sp;
            addRenderableWidget(Button.builder(sp.displayName(), b -> {
                this.selectedSpecies = currentSp;
                refreshButtons();
            }).bounds(left + 10, tabY, 95, 20).build());
            tabY += 24;
        }

        // Action buttons
        int actionY = top + H - 32;

        toggleActiveBtn = addRenderableWidget(Button.builder(Component.literal("Summon"), b -> {
            if (VoidNetwork.CHANNEL != null) {
                VoidNetwork.CHANNEL.sendToServer(new CompanionActionPacket(
                        CompanionActionPacket.Action.TOGGLE_ACTIVE, selectedSpecies, 0));
            }
        }).bounds(left + 115, actionY, 60, 20).build());

        setPrimaryBtn = addRenderableWidget(Button.builder(Component.literal("Primary"), b -> {
            if (VoidNetwork.CHANNEL != null) {
                VoidNetwork.CHANNEL.sendToServer(new CompanionActionPacket(
                        CompanionActionPacket.Action.SELECT, selectedSpecies, 0));
            }
        }).bounds(left + 180, actionY, 55, 20).build());

        evolveBtn = addRenderableWidget(Button.builder(Component.translatable("screen.void_ascension.evolve"), b -> {
            if (VoidNetwork.CHANNEL != null) {
                VoidNetwork.CHANNEL.sendToServer(new CompanionActionPacket(
                        CompanionActionPacket.Action.EVOLVE, selectedSpecies, 0));
            }
        }).bounds(left + 240, actionY, 65, 20).build());

        unlockBtn = addRenderableWidget(Button.builder(Component.literal("Unlock (50 E)"), b -> {
            if (VoidNetwork.CHANNEL != null) {
                VoidNetwork.CHANNEL.sendToServer(new CompanionActionPacket(
                        CompanionActionPacket.Action.UNLOCK, selectedSpecies, 0));
            }
        }).bounds(left + 115, actionY, 120, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 55, top + 6, 45, 18).build());

        refreshButtons();
    }

    @Override
    public void tick() {
        refreshButtons();
    }

    private void refreshButtons() {
        VoidPlayerData d = ClientVoidData.get();
        CompanionData cd = d.getCompanion(selectedSpecies);
        if (cd == null) return;

        boolean unlocked = cd.isUnlocked();
        unlockBtn.visible = !unlocked;
        unlockBtn.active = d.getEssence() >= 50;

        toggleActiveBtn.visible = unlocked;
        setPrimaryBtn.visible = unlocked;
        evolveBtn.visible = unlocked;

        if (unlocked) {
            toggleActiveBtn.setMessage(Component.literal(cd.isActive() ? "Dismiss" : "Summon"));
            boolean canSummon = cd.isActive() || d.getActiveCompanionCount() < d.getMaxActiveCompanions();
            toggleActiveBtn.active = canSummon;

            boolean isPrimary = d.getSelectedCompanion() == selectedSpecies;
            setPrimaryBtn.active = !isPrimary;
            setPrimaryBtn.setMessage(Component.literal(isPrimary ? "Current" : "Select"));

            CompanionStage nextStage = cd.getStage().next();
            if (nextStage != null) {
                boolean levelOk = cd.getLevel() >= nextStage.minLevel();
                boolean tierOk = d.getTier().isAtLeast(nextStage.requiredPlayerTier());
                boolean essenceOk = d.getEssence() >= nextStage.evolutionEssenceCost();
                evolveBtn.active = levelOk && tierOk && essenceOk;
                evolveBtn.setMessage(Component.literal("Evolve (" + nextStage.evolutionEssenceCost() + "E)"));
            } else {
                evolveBtn.active = false;
                evolveBtn.setMessage(Component.literal("Max Stage"));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        // Window background
        g.fill(left, top, left + W, top + H, 0xE5100018);
        g.renderOutline(left, top, W, H, 0xFF7B2CBF);

        // Title
        g.drawString(font, title, left + 12, top + 10, 0xE0AAFF, true);

        VoidPlayerData d = ClientVoidData.get();
        CompanionData cd = d.getCompanion(selectedSpecies);
        if (cd == null) {
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        // Active slots display
        String slotsText = String.format(Locale.ROOT, "Active Slots: %d/%d (%s)",
                d.getActiveCompanionCount(), d.getMaxActiveCompanions(), d.getTier().displayName().getString());
        g.drawString(font, Component.literal(slotsText), left + 115, top + 10, 0x9D4EDD, false);

        int contentX = left + 115;
        int y = top + 26;

        // Species Header
        g.drawString(font, selectedSpecies.displayName(), contentX, y, 0xFFFFFF, true);
        y += 12;
        g.drawString(font, selectedSpecies.roleDescription(), contentX, y, 0xAAAAAA, false);
        y += 14;

        if (!cd.isUnlocked()) {
            g.drawString(font, Component.literal("Status: LOCKED"), contentX, y, 0xFF5555, true);
            y += 14;
            g.drawString(font, Component.literal("Cost to unlock: 50 Void Essence"), contentX, y, 0xC77DFF, false);
            y += 14;
            g.drawString(font, Component.literal("Your Essence: " + d.getEssence()), contentX, y, 0xAAAAAA, false);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        // Stats
        String statusStr = cd.isActive() ? "Active (Summoned)" : "Inactive";
        int statusColor = cd.isActive() ? 0x7FFFD4 : 0x888888;
        g.drawString(font, Component.literal("Status: " + statusStr), contentX, y, statusColor, false);
        y += 11;

        g.drawString(font, Component.literal("Stage: ").append(cd.getStage().displayName()), contentX, y, 0xFFFFFF, false);
        y += 11;

        long nextXp = CompanionData.xpForLevel(cd.getLevel() + 1);
        String levelStr = cd.getLevel() >= 30 ? "Level: 30 (Max)" : String.format(Locale.ROOT, "Level: %d (%d/%d XP)", cd.getLevel(), cd.getXp(), nextXp);
        g.drawString(font, Component.literal(levelStr), contentX, y, 0xD0B0FF, false);
        y += 10;

        // XP bar
        float xpProgress = cd.getLevel() >= 30 ? 1.0F : Math.min(1.0F, cd.getXp() / (float) Math.max(1, nextXp));
        g.fill(contentX, y, contentX + 190, y + 3, 0xFF2A0E3F);
        g.fill(contentX, y, contentX + (int) (190 * xpProgress), y + 3, 0xFF9D4EDD);
        y += 6;

        // Health bar
        String hpStr = String.format(Locale.ROOT, "Health: %.1f / %.1f", cd.getHealth(), cd.getMaxHealth());
        g.drawString(font, Component.literal(hpStr), contentX, y, 0xFF7777, false);
        y += 10;
        float hpProgress = Math.min(1.0F, cd.getHealth() / Math.max(1.0F, cd.getMaxHealth()));
        g.fill(contentX, y, contentX + 190, y + 3, 0xFF3F0E0E);
        g.fill(contentX, y, contentX + (int) (190 * hpProgress), y + 3, 0xFFFF4444);
        y += 8;

        // Abilities List
        g.drawString(font, Component.literal("Abilities:"), contentX, y, 0xFFFFFF, true);
        y += 11;

        List<CompanionAbility> abilities = selectedSpecies.getAbilities();
        for (int i = 0; i < abilities.size(); i++) {
            CompanionAbility ab = abilities.get(i);
            boolean unlocked = cd.isAbilityUnlocked(ab);
            boolean isSelected = cd.getSelectedAbilityIndex() == i;

            MutableComponent line = Component.literal((isSelected ? "► " : "  ") + (unlocked ? "✔ " : "✘ "))
                    .append(ab.displayName());
            if (!unlocked) {
                line.append(Component.literal(" [" + ab.requiredStage().displayName().getString() + "]"));
            } else if (ab.isActive()) {
                line.append(Component.literal(" (" + ab.essenceCost() + "E, " + (ab.cooldownTicks() / 20) + "s)"));
            }

            int textColor = !unlocked ? 0x666666 : (isSelected ? 0x7FFFD4 : 0xDDDDDD);
            g.drawString(font, line, contentX + 2, y, textColor, false);

            // Tooltip on mouse hover
            if (mouseX >= contentX && mouseX <= contentX + 190 && mouseY >= y - 1 && mouseY <= y + 9) {
                g.renderTooltip(font, font.split(ab.description(), 180), mouseX, mouseY);
            }
            y += 11;
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Ability selection click
        int contentX = left + 115;
        int abilityStartY = top + 107;
        CompanionData cd = ClientVoidData.get().getCompanion(selectedSpecies);
        if (cd != null && cd.isUnlocked()) {
            List<CompanionAbility> abilities = selectedSpecies.getAbilities();
            for (int i = 0; i < abilities.size(); i++) {
                int y = abilityStartY + i * 11;
                if (mouseX >= contentX && mouseX <= contentX + 190 && mouseY >= y - 1 && mouseY <= y + 9) {
                    if (cd.isAbilityUnlocked(abilities.get(i))) {
                        cd.setSelectedAbilityIndex(i);
                        if (VoidNetwork.CHANNEL != null) {
                            VoidNetwork.CHANNEL.sendToServer(new CompanionActionPacket(
                                    CompanionActionPacket.Action.SELECT, selectedSpecies, i));
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
