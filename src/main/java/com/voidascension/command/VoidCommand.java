package com.voidascension.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.voidascension.ability.AbilityDefinition;
import com.voidascension.ability.AbilityService;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.config.VoidConfig;
import com.voidascension.core.CoreService;
import com.voidascension.data.PlayerDataSerializer;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.evolution.EvolutionRequirements;
import com.voidascension.evolution.EvolutionService;
import com.voidascension.evolution.VoidTier;
import com.voidascension.progression.ProgressionService;
import com.voidascension.progression.VoidEssenceService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * {@code /void} (alias {@code /voidascension}).
 * <pre>
 * Everyone (level 0)                    Operators (permission level 2)
 *  /void help                            /void status|evolution info &lt;player&gt;
 *  /void status                          /void essence add|remove|set &lt;player&gt; &lt;amount&gt;
 *  /void essence                         /void evolution advance &lt;player&gt;
 *  /void evolution info                  /void evolution set &lt;player&gt; &lt;tier&gt;
 *  /void evolution advance               /void level set &lt;player&gt; &lt;level&gt;
 *  /void ability list                    /void core status|unbind &lt;player&gt;
 *  /void ability use &lt;id&gt;                /void reset &lt;player&gt;
 *  /void core status                     /void debug &lt;player&gt;  (also needs debug.enableDebugCommands)
 * </pre>
 * All gameplay mutations go through the services, so commands obey the same validation as gameplay.
 */
public final class VoidCommand {
    public static final int ADMIN = 2;

    private static final SuggestionProvider<CommandSourceStack> TIERS = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(VoidTier.values()).map(VoidTier::id), b);
    private static final SuggestionProvider<CommandSourceStack> ABILITIES = (ctx, b) ->
            SharedSuggestionProvider.suggest(VoidAbilities.ids(), b);
    private static final SuggestionProvider<CommandSourceStack> COMPANION_SPECIES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(com.voidascension.companion.CompanionSpecies.values())
                    .map(com.voidascension.companion.CompanionSpecies::id), b);
    private static final SuggestionProvider<CommandSourceStack> COMPANION_STAGES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(com.voidascension.companion.CompanionStage.values())
                    .map(com.voidascension.companion.CompanionStage::id), b);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(build("void"));
        dispatcher.register(Commands.literal("voidascension").redirect(root));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .executes(VoidCommand::help)
                .then(Commands.literal("help").executes(VoidCommand::help))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(admin())
                                .executes(ctx -> status(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("essence")
                        .executes(ctx -> essenceGet(ctx, ctx.getSource().getPlayerOrException()))
                        .then(essenceOp("add", VoidEssenceService::addVoidEssence))
                        .then(essenceOp("remove", VoidEssenceService::removeVoidEssence))
                        .then(essenceOp("set", VoidEssenceService::setVoidEssence)))
                .then(Commands.literal("evolution")
                        .then(Commands.literal("info")
                                .executes(ctx -> evolutionInfo(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).requires(admin())
                                        .executes(ctx -> evolutionInfo(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("advance")
                                .executes(ctx -> evolutionAdvance(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).requires(admin())
                                        .executes(ctx -> evolutionAdvance(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set").requires(admin())
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("tier", StringArgumentType.word()).suggests(TIERS)
                                                .executes(VoidCommand::evolutionSet)))))
                .then(Commands.literal("level").requires(admin())
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                .executes(VoidCommand::levelSet)))))
                .then(Commands.literal("core")
                        .then(Commands.literal("status")
                                .executes(ctx -> coreStatus(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).requires(admin())
                                        .executes(ctx -> coreStatus(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("unbind").requires(admin())
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(VoidCommand::coreUnbind))))
                .then(Commands.literal("companion")
                        .then(Commands.literal("info")
                                .executes(ctx -> companionInfo(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).requires(admin())
                                        .executes(ctx -> companionInfo(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("unlock").requires(admin())
                                .then(Commands.argument("species", StringArgumentType.word()).suggests(COMPANION_SPECIES)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(VoidCommand::companionUnlock))))
                        .then(Commands.literal("level").requires(admin())
                                .then(Commands.argument("species", StringArgumentType.word()).suggests(COMPANION_SPECIES)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 30))
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(VoidCommand::companionLevel)))))
                        .then(Commands.literal("stage").requires(admin())
                                .then(Commands.argument("species", StringArgumentType.word()).suggests(COMPANION_SPECIES)
                                        .then(Commands.argument("stage", StringArgumentType.word()).suggests(COMPANION_STAGES)
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(VoidCommand::companionStage))))))
                .then(Commands.literal("ability")
                        .then(Commands.literal("list").executes(VoidCommand::abilityList))
                        .then(Commands.literal("use")
                                .then(Commands.argument("ability", StringArgumentType.word()).suggests(ABILITIES)
                                        .executes(VoidCommand::abilityUse))))
                .then(Commands.literal("reset").requires(admin())
                        .then(Commands.argument("player", EntityArgument.player()).executes(VoidCommand::reset)))
                .then(Commands.literal("debug").requires(src -> src.hasPermission(ADMIN) && VoidConfig.get(VoidConfig.DEBUG_COMMANDS))
                        .then(Commands.argument("player", EntityArgument.player()).executes(VoidCommand::debug)));
    }

    private static java.util.function.Predicate<CommandSourceStack> admin() {
        return src -> src.hasPermission(ADMIN);
    }

    // ------------------------------------------------------------------ player commands

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.translatable("command.void_ascension.help.header").withStyle(ChatFormatting.DARK_PURPLE), false);
        for (String key : new String[]{"status", "essence", "evolution_info", "evolution_advance", "ability_list",
                "ability_use", "core_status"}) {
            src.sendSuccess(() -> Component.translatable("command.void_ascension.help." + key).withStyle(ChatFormatting.GRAY), false);
        }
        if (src.hasPermission(ADMIN)) {
            src.sendSuccess(() -> Component.translatable("command.void_ascension.help.admin").withStyle(ChatFormatting.GOLD), false);
        }
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        for (Component line : StatusFormatter.lines(target)) ctx.getSource().sendSuccess(() -> line, false);
        return 1;
    }

    private static int essenceGet(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        VoidPlayerData data = VoidCapabilities.forPlayer(target);
        if (!data.isInitialized()) return fail(ctx, Component.translatable("message.void_ascension.not_initialized"));
        ctx.getSource().sendSuccess(() -> Component.translatable("command.void_ascension.essence.get",
                target.getDisplayName(), data.getEssence()), false);
        return data.getEssence();
    }

    private static int evolutionInfo(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        VoidPlayerData data = VoidCapabilities.forPlayer(target);
        if (!data.isInitialized()) return fail(ctx, Component.translatable("message.void_ascension.not_initialized"));
        ctx.getSource().sendSuccess(() -> Component.translatable("message.void_ascension.status.tier",
                data.getTier().displayName(), data.getTier().level()), false);
        for (Component line : StatusFormatter.requirementLines(target)) ctx.getSource().sendSuccess(() -> line, false);
        return 1;
    }

    private static int evolutionAdvance(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        EvolutionRequirements.Check before = EvolutionService.check(target);
        EvolutionService.Result r = target == ctx.getSource().getEntity()
                ? EvolutionService.tryAdvanceWithFeedback(target) : EvolutionService.tryAdvance(target);
        if (!r.success()) {
            if (target != ctx.getSource().getEntity()) return fail(ctx, EvolutionService.failureMessage(r, before));
            return 0;
        }
        if (target != ctx.getSource().getEntity()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.void_ascension.evolution.advanced",
                    target.getDisplayName(), VoidCapabilities.forPlayer(target).getTier().displayName()), true);
        }
        return 1;
    }

    private static int abilityList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        VoidPlayerData data = VoidCapabilities.forPlayer(player);
        for (AbilityDefinition def : VoidAbilities.all()) {
            boolean unlocked = AbilityService.isUnlocked(data, def);
            Component line = Component.literal(unlocked ? "✔ " : "✘ ").append(def.displayName())
                    .append(Component.literal(" [" + def.id() + "] "))
                    .append(Component.translatable("ability.void_ascension.type." + def.type().name().toLowerCase(java.util.Locale.ROOT)))
                    .append(Component.literal(" - "))
                    .append(Component.translatable("message.void_ascension.status.ability_requires", def.unlockTier().displayName()))
                    .withStyle(unlocked ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY);
            ctx.getSource().sendSuccess(() -> line, false);
            ctx.getSource().sendSuccess(() -> Component.literal("   ").append(def.description()).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int abilityUse(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "ability");
        AbilityService.UseResult r = AbilityService.use(player, id);
        if (!r.success()) return fail(ctx, AbilityService.failureMessageFor(player, id, r));
        ctx.getSource().sendSuccess(() -> Component.translatable("command.void_ascension.ability.used",
                VoidAbilities.get(id).displayName()), false);
        return 1;
    }

    private static int coreStatus(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        VoidPlayerData data = VoidCapabilities.forPlayer(target);
        String key = !data.isInitialized() ? "message.void_ascension.not_initialized"
                : CoreService.hasActiveCore(target) ? "message.void_ascension.status.core_carried"
                : data.hasBoundCore() ? "message.void_ascension.status.core_not_carried"
                : "message.void_ascension.status.core_none";
        ctx.getSource().sendSuccess(() -> Component.translatable("command.void_ascension.core.status",
                target.getDisplayName(), Component.translatable(key)), false);
        return data.hasBoundCore() ? 1 : 0;
    }

    // ------------------------------------------------------------------ admin commands

    private static LiteralArgumentBuilder<CommandSourceStack> essenceOp(String op,
            java.util.function.BiFunction<ServerPlayer, Integer, VoidEssenceService.Result> fn) {
        return Commands.literal(op).requires(admin())
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(op.equals("set") ? 0 : 1))
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    VoidEssenceService.Result r = fn.apply(target, amount);
                                    if (!r.success()) return fail(ctx, essenceError(r, target));
                                    int now = VoidEssenceService.getVoidEssence(target);
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "command.void_ascension.essence." + op, amount, target.getDisplayName(), now), true);
                                    return now;
                                })));
    }

    private static Component essenceError(VoidEssenceService.Result r, ServerPlayer target) {
        return switch (r) {
            case INVALID_AMOUNT -> Component.translatable("command.void_ascension.error.invalid_amount");
            case INSUFFICIENT_ESSENCE -> Component.translatable("command.void_ascension.error.insufficient",
                    target.getDisplayName(), VoidEssenceService.getVoidEssence(target));
            case NOT_INITIALIZED -> Component.translatable("command.void_ascension.error.not_initialized", target.getDisplayName());
            default -> Component.translatable("message.void_ascension.error");
        };
    }

    private static int evolutionSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String raw = StringArgumentType.getString(ctx, "tier");
        VoidTier tier = VoidTier.parse(raw);
        if (tier == null) return fail(ctx, Component.translatable("command.void_ascension.error.unknown_tier", raw));
        EvolutionService.setTier(target, tier);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.void_ascension.evolution.set",
                target.getDisplayName(), tier.displayName()), true);
        return tier.level();
    }

    private static int levelSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        if (!VoidCapabilities.forPlayer(target).isInitialized()) {
            return fail(ctx, Component.translatable("command.void_ascension.error.not_initialized", target.getDisplayName()));
        }
        int level = ProgressionService.setLevel(target, IntegerArgumentType.getInteger(ctx, "level"));
        ctx.getSource().sendSuccess(() -> Component.translatable("command.void_ascension.level.set",
                target.getDisplayName(), level), true);
        return level;
    }

    private static int coreUnbind(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        if (!CoreService.unbind(target)) {
            return fail(ctx, Component.translatable("command.void_ascension.core.not_bound", target.getDisplayName()));
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.void_ascension.core.unbound", target.getDisplayName()), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ProgressionService.reset(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.void_ascension.reset", target.getDisplayName()), true);
        return 1;
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String nbt = PlayerDataSerializer.write(VoidCapabilities.forPlayer(target)).toString();
        ctx.getSource().sendSuccess(() -> Component.literal(nbt).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int companionInfo(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        VoidPlayerData d = VoidCapabilities.forPlayer(target);
        if (!d.isInitialized()) return fail(ctx, Component.translatable("command.void_ascension.error.not_initialized", target.getDisplayName()));
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("=== Companions for " + target.getGameProfile().getName() + " ===").withStyle(ChatFormatting.DARK_PURPLE), false);
        for (com.voidascension.companion.CompanionData cd : d.getAllCompanions().values()) {
            String status = cd.isUnlocked() ? (cd.isActive() ? " [ACTIVE]" : " [UNLOCKED]") : " [LOCKED]";
            src.sendSuccess(() -> Component.literal("- ").append(cd.getSpecies().displayName())
                    .append(Component.literal(status + " Lv." + cd.getLevel() + " (" + cd.getStage().displayName().getString() + ")")), false);
        }
        return 1;
    }

    private static int companionUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String spName = StringArgumentType.getString(ctx, "species");
        com.voidascension.companion.CompanionSpecies sp = com.voidascension.companion.CompanionSpecies.byId(spName);
        if (sp == null) return fail(ctx, Component.literal("Unknown species: " + spName));
        VoidPlayerData d = VoidCapabilities.forPlayer(target);
        com.voidascension.companion.CompanionData cd = d.getCompanion(sp);
        if (cd != null) {
            cd.setUnlocked(true);
            VoidNetwork.sync(target);
            ctx.getSource().sendSuccess(() -> Component.literal("Unlocked " + sp.id() + " for " + target.getGameProfile().getName()), true);
            return 1;
        }
        return 0;
    }

    private static int companionLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String spName = StringArgumentType.getString(ctx, "species");
        int level = IntegerArgumentType.getInteger(ctx, "level");
        com.voidascension.companion.CompanionSpecies sp = com.voidascension.companion.CompanionSpecies.byId(spName);
        if (sp == null) return fail(ctx, Component.literal("Unknown species: " + spName));
        VoidPlayerData d = VoidCapabilities.forPlayer(target);
        com.voidascension.companion.CompanionData cd = d.getCompanion(sp);
        if (cd != null) {
            cd.setLevel(level);
            cd.setXp(com.voidascension.companion.CompanionData.xpForLevel(level));
            com.voidascension.companion.CompanionService.reconcileCompanions(target);
            VoidNetwork.sync(target);
            ctx.getSource().sendSuccess(() -> Component.literal("Set " + sp.id() + " level to " + level + " for " + target.getGameProfile().getName()), true);
            return level;
        }
        return 0;
    }

    private static int companionStage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String spName = StringArgumentType.getString(ctx, "species");
        String stageName = StringArgumentType.getString(ctx, "stage");
        com.voidascension.companion.CompanionSpecies sp = com.voidascension.companion.CompanionSpecies.byId(spName);
        if (sp == null) return fail(ctx, Component.literal("Unknown species: " + spName));
        com.voidascension.companion.CompanionStage stage = null;
        for (com.voidascension.companion.CompanionStage s : com.voidascension.companion.CompanionStage.values()) {
            if (s.id().equalsIgnoreCase(stageName) || s.name().equalsIgnoreCase(stageName)) {
                stage = s;
                break;
            }
        }
        if (stage == null) return fail(ctx, Component.literal("Unknown stage: " + stageName));
        VoidPlayerData d = VoidCapabilities.forPlayer(target);
        com.voidascension.companion.CompanionData cd = d.getCompanion(sp);
        if (cd != null) {
            cd.setStage(stage);
            cd.recalculateMaxHealth();
            com.voidascension.companion.CompanionService.reconcileCompanions(target);
            VoidNetwork.sync(target);
            final com.voidascension.companion.CompanionStage finalStage = stage;
            ctx.getSource().sendSuccess(() -> Component.literal("Set " + sp.id() + " stage to " + finalStage.id() + " for " + target.getGameProfile().getName()), true);
            return stage.index();
        }
        return 0;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, Component msg) {
        ctx.getSource().sendFailure(msg);
        return 0;
    }

    private VoidCommand() {}

}
