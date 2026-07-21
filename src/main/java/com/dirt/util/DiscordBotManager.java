package com.dirt.util;

import com.dirt.DirtEconomy;
import com.dirt.config.RolesConfig;
import com.dirt.data.CharacterData;
import com.dirt.data.CustomRole;
import com.dirt.data.FamilyData;
import com.dirt.data.MailItem;
import com.dirt.data.NationData;
import com.dirt.data.RegionData;
import com.dirt.data.WarData;
import java.time.Duration;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordBotManager extends ListenerAdapter {
    private final DirtEconomy plugin;
    private JDA jda;
    private final Map<String, UUID> pendingLinkCodes = new ConcurrentHashMap<>();
    private final Map<String, WarDraftState> warDrafts = new ConcurrentHashMap<>();

    public JDA getJda() { return jda; }

    public DiscordBotManager(DirtEconomy plugin) {
        this.plugin = plugin;
        connect();
    }

    private void connect() {
        String token = plugin.getSettings().getDiscordBotToken();
        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("[Discord] No valid bot token configured. Discord integration disabled.");
            return;
        }
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES)
                    .setActivity(Activity.playing("DirtEconomy | /help"))
                    .addEventListeners(this)
                    .build();
            plugin.getLogger().info("[Discord] Bot connecting...");
        } catch (Exception e) {
            plugin.getLogger().severe("[Discord] Failed to connect: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
                    jda.shutdownNow();
                    jda.awaitShutdown(Duration.ofSeconds(5));
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isConnected() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    public String generateLinkCode(UUID playerUuid) {
        pendingLinkCodes.values().removeIf(uuid -> uuid.equals(playerUuid));
        String code = String.valueOf(100000 + new Random().nextInt(900000));
        pendingLinkCodes.put(code, playerUuid);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> pendingLinkCodes.remove(code), 20L * 300);
        return code;
    }

    // ---- Events ----

    @Override
    public void onReady(ReadyEvent event) {
        plugin.getLogger().info("[Discord] Bot connected as " + event.getJDA().getSelfUser().getName());
        registerSlashCommands(event.getJDA());
        postActivity("🤖 Bot Online", "DirtEconomy Discord bot has connected.", 0x2ECC71);
    }

    private void registerSlashCommands(JDA api) {
        var commandList = java.util.List.of(
                Commands.slash("war", "Manage wars from Discord")
                        .addSubcommands(
                                new SubcommandData("declare", "Declare war on another nation")
                                        .addOption(OptionType.STRING, "target", "Name of the nation to attack", true),
                                new SubcommandData("list", "View your nation's active wars"),
                                new SubcommandData("surrender", "Surrender an active war")
                                        .addOption(OptionType.STRING, "nation", "Name of the nation you are at war with", true)
                        ),
                Commands.slash("family", "Manage your family from Discord")
                        .addSubcommands(
                                new SubcommandData("info", "View your family information"),
                                new SubcommandData("bear", "Request to bear a child with your spouse"),
                                new SubcommandData("inheritor", "View or change your inheritor")
                        ),
                Commands.slash("announce", "Send a national broadcast")
                        .addOption(OptionType.STRING, "message", "The announcement message", true),
                Commands.slash("help", "View available DirtEconomy Discord commands"),
                Commands.slash("profile", "View a character's profile")
                        .addOption(OptionType.STRING, "player", "Character name (optional)", false),
                Commands.slash("online", "View online players"),
                Commands.slash("balance", "Check your in-game balance"),
                Commands.slash("pay", "Send money to another player")
                        .addOption(OptionType.STRING, "player", "Character name", true)
                        .addOption(OptionType.NUMBER, "amount", "Amount to send", true),
                Commands.slash("mail", "View your mailbox from Discord"),
                Commands.slash("msg", "Send an in-game message to a player")
                        .addOption(OptionType.STRING, "player", "Character name", true)
                        .addOption(OptionType.STRING, "message", "Your message", true),
                Commands.slash("nation", "View nation information")
                        .addOption(OptionType.STRING, "name", "Nation name (leave blank for yours)", false),
                Commands.slash("leaderboard", "View server wealth and nation leaderboards"),
                Commands.slash("coords", "Get your in-game coordinates")
        );

        String guildId = plugin.getSettings().getDiscordGuildId();
        if (guildId != null && !guildId.isEmpty()) {
            var guild = api.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(commandList).queue(
                        cmds -> plugin.getLogger().info("[Discord] Registered " + cmds.size() + " slash commands to guild " + guild.getName()),
                        err -> plugin.getLogger().warning("[Discord] Failed to register guild commands: " + err.getMessage()));
                api.updateCommands().addCommands(java.util.Collections.emptyList()).queue();
                return;
            }
            plugin.getLogger().warning("[Discord] Guild ID '" + guildId + "' not found. Bot may not be in that server.");
        }
        api.updateCommands().addCommands(commandList).queue(
                cmds -> plugin.getLogger().info("[Discord] Registered " + cmds.size() + " global slash commands (DM-capable)."),
                err -> plugin.getLogger().warning("[Discord] Failed to register global commands: " + err.getMessage()));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Chat bridge: Discord → Minecraft
        if (plugin.getSettings().isDiscordChatBridgeEnabled()) {
            String bridgeChannelId = plugin.getSettings().getDiscordChatBridgeChannelId();
            if (bridgeChannelId != null && !bridgeChannelId.isEmpty()
                    && event.getChannel().getId().equals(bridgeChannelId)) {
                CharacterData cd = resolveCharacter(event.getAuthor().getId());
                String displayName = cd != null ? cd.getFirstName() + " " + cd.getLastName() : event.getAuthor().getName();
                String msg = "§9[Discord] §f" + displayName + "§7: §f" + event.getMessage().getContentDisplay();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
                        p.sendMessage(msg);
                    }
                });
            }
        }

        String linkChannelId = plugin.getSettings().getDiscordLinkChannelId();
        boolean isLinkChannel = linkChannelId != null && !linkChannelId.isEmpty()
                && event.getChannel().getId().equals(linkChannelId);
        boolean isDM = !event.isFromGuild();

        if (!isLinkChannel && !isDM) return;

        String content = event.getMessage().getContentRaw().trim();
        UUID playerUuid = pendingLinkCodes.remove(content);
        if (playerUuid == null) {
            if (isDM && content.matches("\\d{6}")) {
                event.getChannel().sendMessage("❌ Invalid or expired code. Generate a new one with `/d link` in-game.").queue();
            }
            return;
        }

        String discordId = event.getAuthor().getId();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData data = plugin.getCharacterManager().getCharacter(playerUuid);
            if (data == null) {
                event.getChannel().sendMessage("❌ Character not found. The link code may have expired.").queue();
                return;
            }

            data.setDiscordId(discordId);
            plugin.getCharacterManager().saveCharacter(data);

            String charName = data.getFirstName() + " " + data.getLastName();
            event.getChannel().sendMessage("✅ Successfully linked to **" + charName + "**! You'll receive mailbox notifications as DMs.").queue();

            org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerUuid);
            if (player != null) {
                player.sendMessage("§a§lDiscord linked! §7You'll now receive mailbox notifications on Discord.");
            }
        });
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName();
        switch (cmd) {
            case "help" -> handleHelp(event);
            case "war" -> handleWar(event);
            case "family" -> handleFamily(event);
            case "announce" -> handleAnnounce(event);
            case "profile" -> handleProfile(event);
            case "online" -> handleOnline(event);
            case "balance" -> handleBalance(event);
            case "pay" -> handlePay(event);
            case "mail" -> handleMailCommand(event);
            case "msg" -> handleMsg(event);
            case "nation" -> handleNation(event);
            case "leaderboard" -> handleLeaderboard(event);
            case "coords" -> handleCoords(event);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.startsWith("war_confirm:")) {
            handleWarConfirmButton(event);
            return;
        }
        if (id.startsWith("war_cancel:")) {
            warDrafts.remove(event.getUser().getId());
            event.reply("❌ War declaration cancelled.").setEphemeral(true).queue();
            return;
        }
        if (id.startsWith("war_surrender_confirm:")) {
            handleWarSurrenderConfirm(event);
            return;
        }
        if (id.startsWith("war_surrender_cancel:")) {
            event.reply("❌ Surrender cancelled.").setEphemeral(true).queue();
            return;
        }
        if (id.equals("family_bear_confirm")) {
            handleFamilyBearConfirm(event);
            return;
        }
        if (id.equals("family_bear_cancel")) {
            event.reply("❌ Child bearing request cancelled.").setEphemeral(true).queue();
            return;
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String menuId = event.getComponentId();

        if (menuId.equals("war_attacker_regions")) {
            handleWarAttackerRegionSelect(event);
            return;
        }
        if (menuId.equals("war_target_regions")) {
            handleWarTargetRegionSelect(event);
            return;
        }
    }

    // ---- Help ----

    private void handleHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(0x3498DB);
        eb.setTitle("📋 DirtEconomy Discord Commands");
        eb.addField("⚔ /war declare <nation>", "Declare war on another nation (President/VP)", false);
        eb.addField("⚔ /war list", "View your nation's active wars", false);
        eb.addField("⚔ /war surrender <nation>", "Surrender an active war", false);
        eb.addField("👨‍👩‍👧 /family info", "View your family information", false);
        eb.addField("👨‍👩‍👧 /family bear", "Request to bear a child with your spouse", false);
        eb.addField("👨‍👩‍👧 /family inheritor", "View or change your inheritor", false);
        eb.addField("📢 /announce <message>", "Send a national broadcast (President/VP)", false);
        eb.addField("📋 /profile [player]", "View a character's profile", false);
        eb.addField("🟢 /online", "View online players", false);
        eb.addField("💰 /balance", "Check your in-game balance", false);
        eb.addField("💸 /pay <player> <amount>", "Send money to another player", false);
        eb.addField("📬 /mail", "View your mailbox", false);
        eb.addField("💬 /msg <player> <message>", "Send an in-game message", false);
        eb.addField("🏛 /nation [name]", "View nation information", false);
        eb.addField("🏆 /leaderboard", "View server leaderboards", false);
        eb.addField("📍 /coords", "Get your in-game coordinates", false);
        eb.setFooter("You must link your account with /d link in-game first.");
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    // ---- Resolve linked character ----

    private CharacterData resolveCharacter(String discordId) {
        for (CharacterData c : plugin.getCharacterManager().getAllCharacters()) {
            if (discordId.equals(c.getDiscordId())) return c;
        }
        return null;
    }

    // ---- War Commands ----

    private void handleWar(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;

        event.deferReply(true).queue();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData character = resolveCharacter(event.getUser().getId());
            if (character == null) {
                event.getHook().sendMessage("❌ No linked character found. Use `/d link` in-game first.").queue();
                return;
            }

            switch (sub) {
                case "declare" -> handleWarDeclare(event, character);
                case "list" -> handleWarList(event, character);
                case "surrender" -> handleWarSurrender(event, character);
            }
        });
    }

    private void handleWarDeclare(SlashCommandInteractionEvent event, CharacterData character) {
        UUID playerUuid = character.getPlayerUuid();
        RolesConfig rc = plugin.getRolesConfig();

        NationData nation = plugin.getNationManager().getNationByPresident(playerUuid);
        if (nation == null && rc.canVpManageWar()) {
            for (NationData n : plugin.getNationManager().getAllNations()) {
                if (playerUuid.equals(n.getVicePresidentUUID())) {
                    nation = n;
                    break;
                }
            }
        }
        if (nation == null) {
            event.getHook().sendMessage("❌ Only a nation's President or Vice President can declare war.").queue();
            return;
        }

        String targetName = event.getOption("target", OptionMapping::getAsString);
        NationData target = plugin.getNationManager().getNationByName(targetName);
        if (target == null) {
            event.getHook().sendMessage("❌ Nation **" + targetName + "** not found.").queue();
            return;
        }
        if (target.getNationId().equals(nation.getNationId())) {
            event.getHook().sendMessage("❌ You cannot declare war on your own nation.").queue();
            return;
        }

        List<RegionData> ownRegions = plugin.getNationManager().getRegionsByNation(nation.getNationId());
        List<RegionData> enemyRegions = plugin.getNationManager().getRegionsByNation(target.getNationId());

        if (ownRegions.isEmpty()) {
            event.getHook().sendMessage("❌ Your nation has no regions to attack with.").queue();
            return;
        }
        if (enemyRegions.isEmpty()) {
            event.getHook().sendMessage("❌ Target nation has no regions to claim.").queue();
            return;
        }

        WarDraftState draft = new WarDraftState();
        draft.attackingNationId = nation.getNationId();
        draft.defendingNationId = target.getNationId();
        draft.playerUuid = playerUuid;
        warDrafts.put(event.getUser().getId(), draft);

        int maxAttackers = plugin.getSettings().getMaxAttackingRegions();
        StringSelectMenu.Builder menu = StringSelectMenu.create("war_attacker_regions")
                .setPlaceholder("Select your attacking regions (max " + maxAttackers + ")")
                .setMinValues(1)
                .setMaxValues(Math.min(maxAttackers, ownRegions.size()));

        for (RegionData r : ownRegions) {
            menu.addOption(r.getName(), r.getRegionId().toString(),
                    "Chunks: " + r.getClaimedChunks().size());
        }

        event.getHook().sendMessage("**⚔ Declare War on " + target.getName() + "**\nSelect your attacking regions:")
                .addActionRow(menu.build())
                .queue();
    }

    private void handleWarAttackerRegionSelect(StringSelectInteractionEvent event) {
        WarDraftState draft = warDrafts.get(event.getUser().getId());
        if (draft == null) {
            event.reply("❌ War draft expired. Start over with `/war declare`.").setEphemeral(true).queue();
            return;
        }

        draft.attackerRegionIds.clear();
        for (String val : event.getValues()) {
            draft.attackerRegionIds.add(UUID.fromString(val));
        }

        event.deferReply(true).queue();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            List<RegionData> enemyRegions = plugin.getNationManager().getRegionsByNation(draft.defendingNationId);
            if (enemyRegions.isEmpty()) {
                event.getHook().sendMessage("❌ Target nation has no regions to claim.").queue();
                return;
            }

            StringSelectMenu.Builder menu = StringSelectMenu.create("war_target_regions")
                    .setPlaceholder("Select regions to claim")
                    .setMinValues(1)
                    .setMaxValues(enemyRegions.size());

            for (RegionData r : enemyRegions) {
                menu.addOption(r.getName(), r.getRegionId().toString(),
                        "Chunks: " + r.getClaimedChunks().size());
            }

            event.getHook().sendMessage("**Step 2:** Select the enemy regions you want to claim:")
                    .addActionRow(menu.build())
                    .queue();
        });
    }

    private void handleWarTargetRegionSelect(StringSelectInteractionEvent event) {
        WarDraftState draft = warDrafts.get(event.getUser().getId());
        if (draft == null) {
            event.reply("❌ War draft expired. Start over with `/war declare`.").setEphemeral(true).queue();
            return;
        }

        draft.targetRegionIds.clear();
        for (String val : event.getValues()) {
            draft.targetRegionIds.add(UUID.fromString(val));
        }

        event.deferReply(true).queue();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            NationData attacker = plugin.getNationManager().loadNation(draft.attackingNationId);
            NationData defender = plugin.getNationManager().loadNation(draft.defendingNationId);
            double warCost = plugin.getSettings().getWarCost();

            StringBuilder summary = new StringBuilder();
            summary.append("**⚔ War Declaration Summary**\n");
            summary.append("**Attacker:** ").append(attacker != null ? attacker.getName() : "Unknown").append("\n");
            summary.append("**Target:** ").append(defender != null ? defender.getName() : "Unknown").append("\n");
            summary.append("**Cost:** ").append(CurrencyUtil.symbol()).append(String.format("%.0f", warCost)).append("\n\n");

            summary.append("**Attacking Regions:**\n");
            for (UUID rid : draft.attackerRegionIds) {
                RegionData r = plugin.getNationManager().loadRegion(rid);
                if (r != null) summary.append("• ").append(r.getName()).append("\n");
            }
            summary.append("\n**Regions to Claim:**\n");
            for (UUID rid : draft.targetRegionIds) {
                RegionData r = plugin.getNationManager().loadRegion(rid);
                if (r != null) summary.append("• ").append(r.getName()).append("\n");
            }

            event.getHook().sendMessage(summary.toString())
                    .addActionRow(
                            Button.success("war_confirm:" + event.getUser().getId(), "✅ Confirm & Declare War"),
                            Button.danger("war_cancel:" + event.getUser().getId(), "❌ Cancel")
                    )
                    .queue();
        });
    }

    private void handleWarConfirmButton(ButtonInteractionEvent event) {
        WarDraftState draft = warDrafts.remove(event.getUser().getId());
        if (draft == null) {
            event.reply("❌ War draft expired. Start over with `/war declare`.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            NationData attacker = plugin.getNationManager().loadNation(draft.attackingNationId);
            NationData defender = plugin.getNationManager().loadNation(draft.defendingNationId);
            if (attacker == null || defender == null) {
                event.getHook().sendMessage("❌ Nation data not found.").queue();
                return;
            }

            double warCost = plugin.getSettings().getWarCost();
            org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(draft.playerUuid);
            if (!plugin.getEconomy().has(offlinePlayer, warCost)) {
                event.getHook().sendMessage("❌ Insufficient funds. You need **" + CurrencyUtil.symbol()
                        + String.format("%.0f", warCost) + "** to declare war.").queue();
                return;
            }

            plugin.getEconomy().withdrawPlayer(offlinePlayer, warCost);
            WarData war = plugin.getWarManager().createWar(
                    draft.attackingNationId, draft.defendingNationId,
                    draft.attackerRegionIds, draft.targetRegionIds);

            event.getHook().sendMessage("⚔ **War declared against " + defender.getName() + "!**\n"
                    + CurrencyUtil.symbol() + String.format("%.0f", warCost) + " deducted from your balance.").queue();

            notifyWarInGame(war, attacker, defender);

            postActivity("⚔ War Declared",
                    attacker.getName() + " has declared war on " + defender.getName() + "!",
                    0xE74C3C);
        });
    }

    private void notifyWarInGame(WarData war, NationData attacker, NationData defender) {
        List<UUID> defLeaders = new ArrayList<>();
        if (defender.getPresidentUUID() != null) defLeaders.add(defender.getPresidentUUID());
        if (plugin.getRolesConfig().isVicePresidentEnabled() && defender.getVicePresidentUUID() != null)
            defLeaders.add(defender.getVicePresidentUUID());
        if (plugin.getRolesConfig().isSecurityHeadEnabled() && defender.getSecurityHeadUUID() != null)
            defLeaders.add(defender.getSecurityHeadUUID());

        for (UUID uuid : defLeaders) {
            org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);
            if (p == null) continue;
            p.sendMessage("§c⚔ WAR DECLARED! §e" + attacker.getColor1() + attacker.getName()
                    + "§e has declared war on your nation!");
        }

        for (UUID memberUUID : attacker.getMemberUUIDs()) {
            org.bukkit.entity.Player p = plugin.getServer().getPlayer(memberUUID);
            if (p != null) {
                p.sendMessage("§e⚔ Your nation " + attacker.getColor1() + attacker.getName()
                        + "§e has declared war against " + defender.getColor1() + defender.getName() + "§e!");
            }
        }

        for (UUID memberUUID : defender.getMemberUUIDs()) {
            org.bukkit.entity.Player p = plugin.getServer().getPlayer(memberUUID);
            if (p != null) {
                p.sendMessage("§c⚔ " + attacker.getColor1() + attacker.getName()
                        + "§c has declared war on your nation!");
            }
        }
    }

    private void handleWarList(SlashCommandInteractionEvent event, CharacterData character) {
        NationData nation = plugin.getNationManager().getNationByMember(character.getPlayerUuid());
        if (nation == null) {
            event.getHook().sendMessage("❌ You are not a member of any nation.").queue();
            return;
        }

        List<WarData> wars = plugin.getWarManager().getWarsByNation(nation.getNationId());
        if (wars.isEmpty()) {
            event.getHook().sendMessage("✅ Your nation has no active wars.").queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(0xE74C3C);
        eb.setTitle("⚔ Active Wars — " + nation.getName());

        for (WarData war : wars) {
            boolean isAttacker = nation.getNationId().equals(war.getAttackingNationId());
            UUID otherNationId = isAttacker ? war.getDefendingNationId() : war.getAttackingNationId();
            NationData other = plugin.getNationManager().loadNation(otherNationId);
            String otherName = other != null ? other.getName() : "Unknown";
            String role = isAttacker ? "⚔ Attacking" : "🛡 Defending";

            StringBuilder regions = new StringBuilder();
            List<UUID> targetIds = war.getTargetRegionIds();
            for (UUID rid : targetIds) {
                RegionData r = plugin.getNationManager().loadRegion(rid);
                if (r != null) regions.append("• ").append(r.getName()).append("\n");
            }

            eb.addField(role + " vs " + otherName,
                    "Contested regions:\n" + (regions.length() > 0 ? regions.toString() : "None"), false);
        }

        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }

    private void handleWarSurrender(SlashCommandInteractionEvent event, CharacterData character) {
        UUID playerUuid = character.getPlayerUuid();
        NationData nation = plugin.getNationManager().getNationByPresident(playerUuid);
        if (nation == null) {
            event.getHook().sendMessage("❌ Only the nation's President can surrender.").queue();
            return;
        }

        String targetName = event.getOption("nation", OptionMapping::getAsString);
        NationData other = plugin.getNationManager().getNationByName(targetName);
        if (other == null) {
            event.getHook().sendMessage("❌ Nation **" + targetName + "** not found.").queue();
            return;
        }

        WarData war = null;
        for (WarData w : plugin.getWarManager().getWarsByNation(nation.getNationId())) {
            UUID otherNationId = nation.getNationId().equals(w.getAttackingNationId())
                    ? w.getDefendingNationId() : w.getAttackingNationId();
            if (otherNationId.equals(other.getNationId())) {
                war = w;
                break;
            }
        }

        if (war == null) {
            event.getHook().sendMessage("❌ No active war found with **" + other.getName() + "**.").queue();
            return;
        }

        boolean isDefender = nation.getNationId().equals(war.getDefendingNationId());
        StringBuilder msg = new StringBuilder();
        msg.append("⚠ **Surrender Confirmation**\n");
        msg.append("War with: **").append(other.getName()).append("**\n");
        if (isDefender) {
            msg.append("⚠ Surrendering as **defender** will transfer the contested regions to the attacker!\n");
            msg.append("Regions that will be lost:\n");
            for (UUID rid : war.getTargetRegionIds()) {
                RegionData r = plugin.getNationManager().loadRegion(rid);
                if (r != null) msg.append("• ").append(r.getName()).append("\n");
            }
        } else {
            msg.append("Surrendering as **attacker** will end the war with no territory changes.\n");
        }

        event.getHook().sendMessage(msg.toString())
                .addActionRow(
                        Button.danger("war_surrender_confirm:" + war.getWarId(), "☠ Confirm Surrender"),
                        Button.secondary("war_surrender_cancel:" + event.getUser().getId(), "Cancel")
                )
                .queue();
    }

    private void handleWarSurrenderConfirm(ButtonInteractionEvent event) {
        String warIdStr = event.getComponentId().replace("war_surrender_confirm:", "");
        event.deferReply(true).queue();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            UUID warId;
            try {
                warId = UUID.fromString(warIdStr);
            } catch (IllegalArgumentException e) {
                event.getHook().sendMessage("❌ Invalid war reference.").queue();
                return;
            }

            WarData war = plugin.getWarManager().loadWar(warId);
            if (war == null) {
                event.getHook().sendMessage("❌ War no longer exists.").queue();
                return;
            }

            CharacterData character = resolveCharacter(event.getUser().getId());
            if (character == null) {
                event.getHook().sendMessage("❌ No linked character found.").queue();
                return;
            }

            NationData nation = plugin.getNationManager().getNationByPresident(character.getPlayerUuid());
            if (nation == null) {
                event.getHook().sendMessage("❌ Only the President can surrender.").queue();
                return;
            }

            boolean involved = nation.getNationId().equals(war.getAttackingNationId())
                    || nation.getNationId().equals(war.getDefendingNationId());
            if (!involved) {
                event.getHook().sendMessage("❌ Your nation is not involved in this war.").queue();
                return;
            }

            boolean isDefender = nation.getNationId().equals(war.getDefendingNationId());
            if (isDefender) {
                plugin.getWarManager().surrender(war);
                event.getHook().sendMessage("☠ **Your nation has surrendered.** Contested regions have been transferred.").queue();
            } else {
                plugin.getWarManager().deleteWar(war.getWarId());
                event.getHook().sendMessage("☠ **Your nation has surrendered.** The war has ended with no territory changes.").queue();
            }

            NationData otherNation = plugin.getNationManager().loadNation(
                    isDefender ? war.getAttackingNationId() : war.getDefendingNationId());
            if (otherNation != null) {
                for (UUID memberUUID : otherNation.getMemberUUIDs()) {
                    org.bukkit.entity.Player p = plugin.getServer().getPlayer(memberUUID);
                    if (p != null) {
                        p.sendMessage("§a⚔ " + nation.getColor1() + nation.getName()
                                + "§a has surrendered! The war is over.");
                    }
                }
            }
            for (UUID memberUUID : nation.getMemberUUIDs()) {
                org.bukkit.entity.Player p = plugin.getServer().getPlayer(memberUUID);
                if (p != null) {
                    p.sendMessage("§c☠ Your nation has surrendered the war"
                            + (otherNation != null ? " against " + otherNation.getColor1() + otherNation.getName() : "") + "§c.");
                }
            }

            postActivity("☠ War Surrender",
                    nation.getName() + " has surrendered"
                            + (otherNation != null ? " to " + otherNation.getName() : "") + ".",
                    0x95A5A6);
        });
    }

    // ---- Family Commands ----

    private void handleFamily(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;

        event.deferReply(true).queue();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData character = resolveCharacter(event.getUser().getId());
            if (character == null) {
                event.getHook().sendMessage("fffffc2fffffbd No linked character found. Use `/d link` in-game first.").queue();
                return;
            }

            switch (sub) {
                case "info" -> handleFamilyInfo(event, character);
                case "bear" -> handleFamilyBear(event, character);
                case "inheritor" -> handleFamilyInheritor(event, character);
            }
        });
    }

    private void handleFamilyInfo(SlashCommandInteractionEvent event, CharacterData character) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(0xFF69B4);
        eb.setTitle("👨‍👩‍👧 Family — " + character.getFirstName() + " " + character.getLastName());

        if (character.getFamilyId() == null) {
            eb.setDescription("You are not part of any family.");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        FamilyData family = plugin.getFamilyManager().loadFamily(character.getFamilyId());
        if (family == null) {
            eb.setDescription("Family data not found.");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        String role = character.getFamilyRole();
        eb.addField("Role", role != null ? role : "Unknown", true);

        if ("PRIMARY".equals(role) || "SECONDARY".equals(role)) {
            UUID spouseUuid = character.getPlayerUuid().equals(family.getSpouse1())
                    ? family.getSpouse2() : family.getSpouse1();
            String spouseName = getCharacterName(spouseUuid);
            eb.addField("Spouse", spouseName != null ? spouseName : "None", true);
        } else if ("CHILD".equals(role)) {
            String p1 = getCharacterName(family.getSpouse1());
            String p2 = getCharacterName(family.getSpouse2());
            String parents = (p1 != null ? p1 : "Unknown") + (p2 != null ? " & " + p2 : "");
            eb.addField("Parents", parents, true);
        }

        if (!family.getChildren().isEmpty()) {
            StringBuilder children = new StringBuilder();
            for (UUID childId : family.getChildren()) {
                String name = getCharacterName(childId);
                if (name != null) children.append("• ").append(name).append("\n");
            }
            if (children.length() > 0) {
                eb.addField("Children (" + family.getChildren().size() + ")", children.toString(), false);
            }
        }

        if (character.getInheritorUuid() != null) {
            String inheritorName = getCharacterName(character.getInheritorUuid());
            eb.addField("Inheritor", inheritorName != null ? inheritorName : "Unknown", true);
        }

        if (character.getBirthFamilyId() != null && !character.getBirthFamilyId().equals(character.getFamilyId())) {
            FamilyData birthFamily = plugin.getFamilyManager().loadFamily(character.getBirthFamilyId());
            if (birthFamily != null) {
                String bp1 = getCharacterName(birthFamily.getSpouse1());
                String bp2 = getCharacterName(birthFamily.getSpouse2());
                eb.addField("Birth Family", (bp1 != null ? bp1 : "?") + (bp2 != null ? " & " + bp2 : ""), false);
            }
        }

        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }

    private void handleFamilyBear(SlashCommandInteractionEvent event, CharacterData character) {
        if (character.getFamilyId() == null) {
            event.getHook().sendMessage("❌ You are not part of a family.").queue();
            return;
        }

        String role = character.getFamilyRole();
        if (!"PRIMARY".equals(role) && !"SECONDARY".equals(role)) {
            event.getHook().sendMessage("❌ Only married characters can bear children.").queue();
            return;
        }

        FamilyData family = plugin.getFamilyManager().loadFamily(character.getFamilyId());
        if (family == null) {
            event.getHook().sendMessage("❌ Family data not found.").queue();
            return;
        }

        UUID spouseUuid = character.getPlayerUuid().equals(family.getSpouse1())
                ? family.getSpouse2() : family.getSpouse1();
        String spouseName = getCharacterName(spouseUuid);

        event.getHook().sendMessage("👶 **Bear a Child**\nThis will send a request to your spouse **"
                        + (spouseName != null ? spouseName : "Unknown") + "** to add a child to your family.\n\nConfirm?")
                .addActionRow(
                        Button.success("family_bear_confirm", "✅ Send Request"),
                        Button.danger("family_bear_cancel", "❌ Cancel")
                )
                .queue();
    }

    private void handleFamilyBearConfirm(ButtonInteractionEvent event) {
        event.deferReply(true).queue();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData character = resolveCharacter(event.getUser().getId());
            if (character == null || character.getFamilyId() == null) {
                event.getHook().sendMessage("❌ Character or family not found.").queue();
                return;
            }

            FamilyData family = plugin.getFamilyManager().loadFamily(character.getFamilyId());
            if (family == null) {
                event.getHook().sendMessage("❌ Family data not found.").queue();
                return;
            }

            UUID spouseUuid = character.getPlayerUuid().equals(family.getSpouse1())
                    ? family.getSpouse2() : family.getSpouse1();

            String senderName = character.getFirstName() + " " + character.getLastName();

            MailItem mail = new MailItem();
            mail.setId(UUID.randomUUID().toString());
            mail.setType("CHILD_BEARING_REQUEST");
            mail.setFromPlayerUuid(character.getPlayerUuid());
            mail.setTimestamp(System.currentTimeMillis());
            Map<String, String> data = new HashMap<>();
            data.put("fromName", senderName);
            data.put("familyId", character.getFamilyId().toString());
            mail.setData(data);

            plugin.getCharacterManager().addMailItem(spouseUuid, mail);

            event.getHook().sendMessage("✅ Child bearing request sent to your spouse!").queue();

            org.bukkit.entity.Player spousePlayer = plugin.getServer().getPlayer(spouseUuid);
            if (spousePlayer != null) {
                spousePlayer.sendMessage("§d" + senderName + " §7wants to bear a child with you! Check your mailbox.");
            }
        });
    }

    private void handleFamilyInheritor(SlashCommandInteractionEvent event, CharacterData character) {
        if (character.getFamilyId() == null) {
            event.getHook().sendMessage("❌ You are not part of a family.").queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(0xF1C40F);
        eb.setTitle("🏆 Inheritor — " + character.getFirstName() + " " + character.getLastName());

        if (character.getInheritorUuid() != null) {
            String inheritorName = getCharacterName(character.getInheritorUuid());
            eb.setDescription("Your current inheritor is **" + (inheritorName != null ? inheritorName : "Unknown") + "**.");
        } else {
            eb.setDescription("You have not set an inheritor. Use the in-game GUI (`/dc` → Family → Manage Inheritor) to select one.");
        }

        FamilyData family = plugin.getFamilyManager().loadFamily(character.getFamilyId());
        if (family != null) {
            StringBuilder eligible = new StringBuilder();
            UUID spouseUuid = character.getPlayerUuid().equals(family.getSpouse1())
                    ? family.getSpouse2() : family.getSpouse1();
            if (spouseUuid != null) {
                String name = getCharacterName(spouseUuid);
                if (name != null) eligible.append("• ").append(name).append(" (Spouse)\n");
            }
            for (UUID childId : family.getChildren()) {
                String name = getCharacterName(childId);
                if (name != null) eligible.append("• ").append(name).append(" (Child)\n");
            }
            if (eligible.length() > 0) {
                eb.addField("Eligible Inheritors", eligible.toString(), false);
            }
        }

        eb.setFooter("Use /dc → Family → Manage Inheritor in-game to change your inheritor.");
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }

    // ---- Announce Command ----

    private void handleAnnounce(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData character = resolveCharacter(event.getUser().getId());
            if (character == null) {
                event.getHook().sendMessage("❌ No linked character found. Use `/d link` in-game first.").queue();
                return;
            }

            UUID playerUuid = character.getPlayerUuid();
            RolesConfig rc = plugin.getRolesConfig();

            NationData nation = plugin.getNationManager().getNationByPresident(playerUuid);
            if (nation == null && rc.canVpManageAnnouncements()) {
                for (NationData n : plugin.getNationManager().getAllNations()) {
                    if (playerUuid.equals(n.getVicePresidentUUID())) {
                        nation = n;
                        break;
                    }
                }
            }
            if (nation == null) {
                event.getHook().sendMessage("❌ Only a nation's President or Vice President can make announcements.").queue();
                return;
            }

            String message = event.getOption("message", OptionMapping::getAsString);
            if (message == null || message.isBlank()) {
                event.getHook().sendMessage("❌ Message cannot be empty.").queue();
                return;
            }

            String senderName = character.getFirstName() + " " + character.getLastName();
            String header = nation.getColor1() + "§l[" + nation.getName() + " Announcement]";
            String body = "§f" + message;
            String footer = "§8— " + senderName;

            int delivered = 0;
            for (UUID memberUUID : nation.getMemberUUIDs()) {
                org.bukkit.entity.Player member = plugin.getServer().getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage("");
                    member.sendMessage(header);
                    member.sendMessage(body);
                    member.sendMessage(footer);
                    member.sendMessage("");
                    delivered++;
                }
            }

            event.getHook().sendMessage("📢 **Announcement sent!**\nDelivered to **" + delivered + "** online members out of **"
                    + nation.getMemberUUIDs().size() + "** total.").queue();
        });
    }

    // ---- Mail Notification ----

    public void sendMailNotification(UUID playerUuid, MailItem mail) {
        if (jda == null) return;

        CharacterData data = plugin.getCharacterManager().getCharacter(playerUuid);
        if (data == null || data.getDiscordId() == null) return;

        EmbedBuilder embed = buildMailEmbed(mail, data);
        if (embed == null) return;

        jda.retrieveUserById(data.getDiscordId()).queue(
                user -> user.openPrivateChannel().queue(
                        channel -> channel.sendMessageEmbeds(embed.build()).queue(null, error -> {}),
                        error -> {}
                ),
                error -> {}
        );
    }

    private EmbedBuilder buildMailEmbed(MailItem mail, CharacterData recipient) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTimestamp(Instant.ofEpochMilli(mail.getTimestamp()));
        eb.setFooter(recipient.getFirstName() + " " + recipient.getLastName() + "'s Mailbox");

        Map<String, String> d = mail.getData();

        switch (mail.getType()) {
            case "MARRIAGE_REQUEST" -> {
                eb.setColor(0xFF69B4);
                eb.setTitle("\uD83D\uDC8D Marriage Request");
                eb.addField("From", d.getOrDefault("fromName", "Unknown"), true);
                eb.addField("Their Balance", d.getOrDefault("fromBalance", "0"), true);
                eb.setDescription("Someone wants to marry your character! Log in to accept or deny.");
            }
            case "CHILD_BEARING_REQUEST" -> {
                eb.setColor(0x57F287);
                eb.setTitle("\uD83D\uDC76 Bear a Child Request");
                eb.addField("From", d.getOrDefault("fromName", "Unknown"), true);
                eb.setDescription("Your spouse wants to add a child to your family.");
            }
            case "CHILD_JOIN_REQUEST" -> {
                eb.setColor(0x57F287);
                eb.setTitle("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67 Family Join Request");
                eb.addField("From", d.getOrDefault("fromName", "Unknown"), true);
                eb.setDescription("Someone wants you to become a child in their family.");
            }
            case "NATION_INVITE" -> {
                eb.setColor(0xFEE75C);
                eb.setTitle("\uD83C\uDFF0 Nation Invite");
                eb.addField("Nation", d.getOrDefault("nationName", "Unknown"), true);
                eb.addField("Invited By", d.getOrDefault("inviterName", "Unknown"), true);
                eb.setDescription("You've been invited to join a nation.");
            }
            case "JOB_OFFER" -> {
                eb.setColor(0xE67E22);
                eb.setTitle("\uD83D\uDCBC Job Offer");
                eb.addField("Business", d.getOrDefault("businessName", "Unknown"), true);
                eb.addField("Owner", d.getOrDefault("ownerName", "Unknown"), true);
                eb.addField("Weekly Pay", d.getOrDefault("payrollRate", "0"), true);
                eb.setDescription("You've received a job offer.");
            }
            case "JOB_APPLICATION" -> {
                eb.setColor(0x3498DB);
                eb.setTitle("\uD83D\uDCCB Job Application");
                eb.addField("Business", d.getOrDefault("businessName", "Unknown"), true);
                eb.addField("Applicant", d.getOrDefault("applicantName", "Unknown"), true);
                eb.setDescription("Someone applied to work at your business.");
            }
            case "BUSINESS_SALE_REQUEST" -> {
                eb.setColor(0xF1C40F);
                eb.setTitle("\uD83C\uDFEA Business Purchase Request");
                eb.addField("Business", d.getOrDefault("businessName", "Unknown"), true);
                eb.addField("Buyer", d.getOrDefault("buyerName", "Unknown"), true);
                eb.addField("Price", d.getOrDefault("salePrice", "0"), true);
                eb.setDescription("Someone wants to purchase your business.");
            }
            case "PROPERTY_ENTRY" -> {
                eb.setColor(0x3498DB);
                eb.setTitle("\uD83D\uDEAA Property Entry");
                eb.addField("Property", d.getOrDefault("propertyName", "Unknown"), true);
                eb.addField("Entered By", d.getOrDefault("entrantName", "Unknown"), true);
                eb.setDescription("Someone entered your property.");
            }
            default -> {
                eb.setColor(0x95A5A6);
                eb.setTitle("\uD83D\uDCE8 New Mail");
                eb.setDescription("You have a new message in your mailbox. Log in to view it.");
            }
        }

        return eb;
    }

    // ---- Helpers ----

    private String getCharacterName(UUID uuid) {
        if (uuid == null) return null;
        CharacterData c = plugin.getCharacterManager().getCharacter(uuid);
        if (c != null) return c.getFirstName() + " " + c.getLastName();
        return null;
    }

    // ---- War Draft State ----

    private static class WarDraftState {
        UUID attackingNationId;
        UUID defendingNationId;
        UUID playerUuid;
        List<UUID> attackerRegionIds = new ArrayList<>();
        List<UUID> targetRegionIds = new ArrayList<>();
    }

    // ---- Discord Role / Nickname Sync ----

    private static final Map<String, Integer> MC_COLOR_MAP = new HashMap<>();
    static {
        MC_COLOR_MAP.put("\u00a70", 0x000001);
        MC_COLOR_MAP.put("\u00a71", 0x0000AA);
        MC_COLOR_MAP.put("\u00a72", 0x00AA00);
        MC_COLOR_MAP.put("\u00a73", 0x00AAAA);
        MC_COLOR_MAP.put("\u00a74", 0xAA0000);
        MC_COLOR_MAP.put("\u00a75", 0xAA00AA);
        MC_COLOR_MAP.put("\u00a76", 0xFFAA00);
        MC_COLOR_MAP.put("\u00a77", 0xAAAAAA);
        MC_COLOR_MAP.put("\u00a78", 0x555555);
        MC_COLOR_MAP.put("\u00a79", 0x5555FF);
        MC_COLOR_MAP.put("\u00a7a", 0x55FF55);
        MC_COLOR_MAP.put("\u00a7b", 0x55FFFF);
        MC_COLOR_MAP.put("\u00a7c", 0xFF5555);
        MC_COLOR_MAP.put("\u00a7d", 0xFF55FF);
        MC_COLOR_MAP.put("\u00a7e", 0xFFFF55);
        MC_COLOR_MAP.put("\u00a7f", 0xFFFFFF);
    }

    private int mcColorToRgb(String colorCode) {
        Integer rgb = MC_COLOR_MAP.get(colorCode);
        return rgb != null ? rgb : 0xFFFFFF;
    }

    private String buildDiscordRoleName(NationData nation, CustomRole role) {
        return nation.getName() + ", " + role.getName();
    }

    public void syncAllDiscordData() {
        if (!isConnected()) return;
        String guildId = plugin.getSettings().getDiscordGuildId();
        if (guildId == null || guildId.isEmpty()) return;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        if (plugin.getSettings().isDiscordRolePermissions()) {
            syncDiscordRoles(guild);
        }
        if (plugin.getSettings().isDiscordDirtcName()) {
            syncDiscordNicknames(guild);
        }
    }

    private void syncDiscordRoles(Guild guild) {
        Set<String> expectedRoleNames = new HashSet<>();
        Map<String, Integer> expectedColors = new HashMap<>();
        Map<String, Set<String>> roleNameToDiscordIds = new HashMap<>();

        for (NationData nation : plugin.getNationManager().getAllNations()) {
            int nationColor = mcColorToRgb(nation.getColor1());

            String nationRoleName = nation.getName();
            expectedRoleNames.add(nationRoleName);
            expectedColors.put(nationRoleName, nationColor);
            Set<String> nationMemberIds = new HashSet<>();
            for (UUID memberUuid : nation.getMemberUUIDs()) {
                CharacterData c = plugin.getCharacterManager().getCharacter(memberUuid);
                if (c != null && c.getDiscordId() != null) {
                    nationMemberIds.add(c.getDiscordId());
                }
            }
            roleNameToDiscordIds.put(nationRoleName, nationMemberIds);

            collectPresetRole(nation, "President", nation.getPresidentUUID(), expectedRoleNames, expectedColors, roleNameToDiscordIds);
            collectPresetRole(nation, "Vice President", nation.getVicePresidentUUID(), expectedRoleNames, expectedColors, roleNameToDiscordIds);
            collectPresetRole(nation, "Treasurer", nation.getTreasurerUUID(), expectedRoleNames, expectedColors, roleNameToDiscordIds);
            collectPresetRole(nation, "Security Head", nation.getSecurityHeadUUID(), expectedRoleNames, expectedColors, roleNameToDiscordIds);

            for (CustomRole role : nation.getCustomRoles()) {
                String roleName = buildDiscordRoleName(nation, role);
                expectedRoleNames.add(roleName);
                expectedColors.put(roleName, nationColor);

                Set<String> discordIds = new HashSet<>();
                for (UUID memberUuid : role.getMemberUUIDs()) {
                    CharacterData c = plugin.getCharacterManager().getCharacter(memberUuid);
                    if (c != null && c.getDiscordId() != null) {
                        discordIds.add(c.getDiscordId());
                    }
                }
                roleNameToDiscordIds.put(roleName, discordIds);
            }
        }

        guild.loadMembers().onSuccess(members -> {
            Map<String, Role> existingRoles = new HashMap<>();
            for (Role r : guild.getRoles()) {
                if (!r.isManaged() && !r.isPublicRole()) {
                    existingRoles.put(r.getName(), r);
                }
            }

            for (String roleName : expectedRoleNames) {
                Role existing = existingRoles.get(roleName);
                int color = expectedColors.getOrDefault(roleName, 0xFFFFFF);
                Set<String> targetIds = roleNameToDiscordIds.getOrDefault(roleName, Set.of());

                if (existing == null) {
                    guild.createRole()
                            .setName(roleName)
                            .setColor(color)
                            .setMentionable(false)
                            .setHoisted(false)
                            .queue(created -> assignRoleToMembers(guild, created, targetIds, members));
                } else {
                    if (existing.getColorRaw() != color) {
                        existing.getManager().setColor(color).queue();
                    }
                    assignRoleToMembers(guild, existing, targetIds, members);
                    existingRoles.remove(roleName);
                }
            }

            for (Map.Entry<String, Role> entry : existingRoles.entrySet()) {
                String name = entry.getKey();
                if (name.contains(", ") && !expectedRoleNames.contains(name)) {
                    boolean wasManaged = false;
                    for (NationData n : plugin.getNationManager().getAllNations()) {
                        if (name.startsWith(n.getName() + ", ")) {
                            wasManaged = true;
                            break;
                        }
                    }
                    if (wasManaged) {
                        entry.getValue().delete().queue(null, err -> {});
                    }
                }
            }
        });
    }

    private void collectPresetRole(NationData nation, String roleTitle, UUID holder,
                                   Set<String> expectedRoleNames, Map<String, Integer> expectedColors,
                                   Map<String, Set<String>> roleNameToDiscordIds) {
        if (holder == null) return;
        String roleName = nation.getName() + ", " + roleTitle;
        expectedRoleNames.add(roleName);
        expectedColors.put(roleName, mcColorToRgb(nation.getColor1()));
        Set<String> ids = new HashSet<>();
        CharacterData c = plugin.getCharacterManager().getCharacter(holder);
        if (c != null && c.getDiscordId() != null) {
            ids.add(c.getDiscordId());
        }
        roleNameToDiscordIds.put(roleName, ids);
    }

    private void assignRoleToMembers(Guild guild, Role role, Set<String> targetDiscordIds, List<Member> members) {
        for (Member member : members) {
            boolean shouldHave = targetDiscordIds.contains(member.getId());
            boolean hasRole = member.getRoles().contains(role);
            if (shouldHave && !hasRole) {
                guild.addRoleToMember(member, role).queue(null, err -> {});
            } else if (!shouldHave && hasRole) {
                guild.removeRoleFromMember(member, role).queue(null, err -> {});
            }
        }
    }

    private void syncDiscordNicknames(Guild guild) {
        guild.loadMembers().onSuccess(members -> {
            Map<String, String> discordIdToCharName = new HashMap<>();
            for (CharacterData c : plugin.getCharacterManager().getAllCharacters()) {
                if (c.getDiscordId() != null) {
                    discordIdToCharName.put(c.getDiscordId(), c.getFirstName() + " " + c.getLastName());
                }
            }

            for (Member member : members) {
                if (member.getUser().isBot()) continue;
                String charName = discordIdToCharName.get(member.getId());
                if (charName != null) {
                    String current = member.getNickname();
                    if (!charName.equals(current)) {
                        member.modifyNickname(charName).queue(null, err -> {});
                    }
                }
            }
        });
    }

    public void onNationCreated(NationData nation) {
        if (!isConnected() || !plugin.getSettings().isDiscordRolePermissions()) return;
        String guildId = plugin.getSettings().getDiscordGuildId();
        if (guildId == null || guildId.isEmpty()) return;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        syncDiscordRoles(guild);
    }

    public void onNationDeleted(NationData nation) {
        if (!isConnected() || !plugin.getSettings().isDiscordRolePermissions()) return;
        String guildId = plugin.getSettings().getDiscordGuildId();
        if (guildId == null || guildId.isEmpty()) return;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        for (Role r : guild.getRolesByName(nation.getName(), false)) {
            r.delete().queue(null, err -> {});
        }
        String prefix = nation.getName() + ", ";
        for (Role r : guild.getRoles()) {
            if (!r.isManaged() && !r.isPublicRole() && r.getName().startsWith(prefix)) {
                r.delete().queue(null, err -> {});
            }
        }
    }

    public void onCustomRoleCreated(NationData nation, CustomRole role) {
        if (!isConnected() || !plugin.getSettings().isDiscordRolePermissions()) return;
        String guildId = plugin.getSettings().getDiscordGuildId();
        if (guildId == null || guildId.isEmpty()) return;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        String roleName = buildDiscordRoleName(nation, role);
        int color = mcColorToRgb(nation.getColor1());
        guild.createRole().setName(roleName).setColor(color).setMentionable(false).setHoisted(false).queue(null, err -> {});
    }

    public void onCustomRoleDeleted(NationData nation, CustomRole role) {
        if (!isConnected() || !plugin.getSettings().isDiscordRolePermissions()) return;
        String guildId = plugin.getSettings().getDiscordGuildId();
        if (guildId == null || guildId.isEmpty()) return;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        String roleName = buildDiscordRoleName(nation, role);
        for (Role r : guild.getRolesByName(roleName, false)) {
            r.delete().queue(null, err -> {});
        }
    }

    public void onCustomRoleUpdated(NationData nation, CustomRole role) {
        if (!isConnected() || !plugin.getSettings().isDiscordRolePermissions()) return;
        String guildId = plugin.getSettings().getDiscordGuildId();
        if (guildId == null || guildId.isEmpty()) return;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        syncDiscordRoles(guild);
    }

    // ---- Character Search ----

    private CharacterData findCharacterByName(String name) {
        if (name == null || name.isEmpty()) return null;
        String lower = name.toLowerCase().trim();
        for (CharacterData c : plugin.getCharacterManager().getAllCharacters()) {
            String fullName = (c.getFirstName() + " " + c.getLastName()).toLowerCase();
            if (fullName.equals(lower)) return c;
        }
        for (CharacterData c : plugin.getCharacterManager().getAllCharacters()) {
            String first = c.getFirstName() != null ? c.getFirstName().toLowerCase() : "";
            String last = c.getLastName() != null ? c.getLastName().toLowerCase() : "";
            if (first.equals(lower) || last.equals(lower)) return c;
        }
        return null;
    }

    // ---- Profile Command ----

    private void handleProfile(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String targetName = event.getOption("player", OptionMapping::getAsString);
            CharacterData character;
            if (targetName != null && !targetName.isEmpty()) {
                character = findCharacterByName(targetName);
            } else {
                character = resolveCharacter(event.getUser().getId());
            }
            if (character == null) {
                event.getHook().sendMessage("❌ Character not found. Specify a name or link your account.").queue();
                return;
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(0x2ECC71);
            eb.setTitle("📋 " + character.getFirstName() + " " + character.getLastName());
            eb.addField("Gender", character.getGender() != null ? character.getGender() : "Unknown", true);
            eb.addField("Status", character.isAlive() ? "✅ Alive" : "💀 Deceased", true);

            org.bukkit.OfflinePlayer offp = plugin.getServer().getOfflinePlayer(character.getPlayerUuid());
            double balance = plugin.getEconomy().getBalance(offp);
            eb.addField("Balance", CurrencyUtil.symbol() + String.format("%.2f", balance), true);

            org.bukkit.entity.Player online = plugin.getServer().getPlayer(character.getPlayerUuid());
            eb.addField("Online", online != null && online.isOnline() ? "🟢 Yes" : "🔴 No", true);

            if (character.getFamilyId() != null) {
                String role = character.getFamilyRole();
                eb.addField("Family Role", role != null ? role : "Member", true);
            }

            if (plugin.isDirtNationsEnabled()) {
                NationData nation = plugin.getNationManager().getNationByMember(character.getPlayerUuid());
                if (nation != null) {
                    eb.addField("Nation", nation.getName(), true);
                }
            }

            eb.addField("Discord", character.getDiscordId() != null ? "🔗 Linked" : "❌ Not linked", true);
            eb.addField("Mail", character.getMailbox().size() + " items", true);

            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }

    // ---- Online Command ----

    private void handleOnline(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            var players = plugin.getServer().getOnlinePlayers();
            if (players.isEmpty()) {
                event.getHook().sendMessage("No players currently online.").queue();
                return;
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(0x3498DB);
            eb.setTitle("🟢 Online Players (" + players.size() + "/" + plugin.getServer().getMaxPlayers() + ")");
            StringBuilder sb = new StringBuilder();
            for (var p : players) {
                CharacterData cd = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                if (cd != null) {
                    sb.append("• **").append(cd.getFirstName()).append(" ").append(cd.getLastName()).append("**\n");
                } else {
                    sb.append("• ").append(p.getName()).append(" *(creating character)*\n");
                }
            }
            eb.setDescription(sb.toString());
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }

    // ---- Balance Command ----

    private void handleBalance(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData character = resolveCharacter(event.getUser().getId());
            if (character == null) {
                event.getHook().sendMessage("❌ No linked character found. Use `/d link` in-game first.").queue();
                return;
            }
            org.bukkit.OfflinePlayer offp = plugin.getServer().getOfflinePlayer(character.getPlayerUuid());
            double balance = plugin.getEconomy().getBalance(offp);
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(0xF1C40F);
            eb.setTitle("💰 Balance — " + character.getFirstName() + " " + character.getLastName());
            eb.setDescription("**" + CurrencyUtil.symbol() + String.format("%.2f", balance) + "**");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }

    // ---- Pay Command ----

    private void handlePay(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData sender = resolveCharacter(event.getUser().getId());
            if (sender == null) {
                event.getHook().sendMessage("❌ No linked character found. Use `/d link` in-game first.").queue();
                return;
            }
            String targetName = event.getOption("player", OptionMapping::getAsString);
            double amount = event.getOption("amount", OptionMapping::getAsDouble);
            if (amount <= 0) {
                event.getHook().sendMessage("❌ Amount must be positive.").queue();
                return;
            }
            CharacterData target = findCharacterByName(targetName);
            if (target == null) {
                event.getHook().sendMessage("❌ Character **" + targetName + "** not found.").queue();
                return;
            }
            if (target.getPlayerUuid().equals(sender.getPlayerUuid())) {
                event.getHook().sendMessage("❌ You cannot pay yourself.").queue();
                return;
            }
            org.bukkit.OfflinePlayer senderOffline = plugin.getServer().getOfflinePlayer(sender.getPlayerUuid());
            if (!plugin.getEconomy().has(senderOffline, amount)) {
                event.getHook().sendMessage("❌ Insufficient funds.").queue();
                return;
            }
            org.bukkit.OfflinePlayer targetOffline = plugin.getServer().getOfflinePlayer(target.getPlayerUuid());
            plugin.getEconomy().withdrawPlayer(senderOffline, amount);
            plugin.getEconomy().depositPlayer(targetOffline, amount);
            String senderName = sender.getFirstName() + " " + sender.getLastName();
            String recipientName = target.getFirstName() + " " + target.getLastName();
            event.getHook().sendMessage("✅ Sent **" + CurrencyUtil.symbol() + String.format("%.2f", amount)
                    + "** to **" + recipientName + "**.").queue();
            org.bukkit.entity.Player recipientOnline = plugin.getServer().getPlayer(target.getPlayerUuid());
            if (recipientOnline != null) {
                recipientOnline.sendMessage("§a" + senderName + " sent you §e" + CurrencyUtil.symbol()
                        + String.format("%.2f", amount) + "§a via Discord.");
            }
            postActivity("💸 Payment", senderName + " sent " + CurrencyUtil.symbol()
                    + String.format("%.2f", amount) + " to " + recipientName, 0xF1C40F);
        });
    }

    // ---- Mail Command ----

    private void handleMailCommand(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData character = resolveCharacter(event.getUser().getId());
            if (character == null) {
                event.getHook().sendMessage("❌ No linked character found. Use `/d link` in-game first.").queue();
                return;
            }
            List<MailItem> mail = character.getMailbox();
            if (mail.isEmpty()) {
                event.getHook().sendMessage("📭 Your mailbox is empty.").queue();
                return;
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(0x9B59B6);
            eb.setTitle("📬 Mailbox — " + character.getFirstName() + " " + character.getLastName()
                    + " (" + mail.size() + " items)");
            int shown = Math.min(mail.size(), 10);
            for (int i = 0; i < shown; i++) {
                MailItem item = mail.get(i);
                String fromName = item.getData() != null ? item.getData().getOrDefault("fromName", "Unknown") : "Unknown";
                String type = item.getType().replace("_", " ");
                long ageMinutes = (System.currentTimeMillis() - item.getTimestamp()) / (1000L * 60L);
                String ageStr;
                if (ageMinutes < 60) ageStr = ageMinutes + "m ago";
                else if (ageMinutes < 1440) ageStr = (ageMinutes / 60) + "h ago";
                else ageStr = (ageMinutes / 1440) + "d ago";
                eb.addField(type, "From: " + fromName + " — " + ageStr, false);
            }
            if (mail.size() > 10) {
                eb.setFooter("Showing 10 of " + mail.size() + " items. View all in-game.");
            }
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }

    // ---- Message Command ----

    private void handleMsg(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData sender = resolveCharacter(event.getUser().getId());
            if (sender == null) {
                event.getHook().sendMessage("❌ No linked character found. Use `/d link` in-game first.").queue();
                return;
            }
            String targetName = event.getOption("player", OptionMapping::getAsString);
            String message = event.getOption("message", OptionMapping::getAsString);
            CharacterData target = findCharacterByName(targetName);
            if (target == null) {
                event.getHook().sendMessage("❌ Character **" + targetName + "** not found.").queue();
                return;
            }
            org.bukkit.entity.Player online = plugin.getServer().getPlayer(target.getPlayerUuid());
            if (online == null || !online.isOnline()) {
                event.getHook().sendMessage("❌ **" + target.getFirstName() + " " + target.getLastName() + "** is not online.").queue();
                return;
            }
            String senderName = sender.getFirstName() + " " + sender.getLastName();
            online.sendMessage("§d[Discord DM] §f" + senderName + "§7: §f" + message);
            event.getHook().sendMessage("✅ Message sent to **" + target.getFirstName() + " " + target.getLastName() + "**.").queue();
        });
    }

    // ---- Nation Command ----

    private void handleNation(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!plugin.isDirtNationsEnabled()) {
                event.getHook().sendMessage("❌ Nations are not enabled on this server.").queue();
                return;
            }
            String nationName = event.getOption("name", OptionMapping::getAsString);
            NationData nation;
            if (nationName != null && !nationName.isEmpty()) {
                nation = plugin.getNationManager().getNationByName(nationName);
            } else {
                CharacterData cd = resolveCharacter(event.getUser().getId());
                if (cd == null) {
                    event.getHook().sendMessage("❌ No linked character. Specify a nation name or link your account.").queue();
                    return;
                }
                nation = plugin.getNationManager().getNationByMember(cd.getPlayerUuid());
            }
            if (nation == null) {
                event.getHook().sendMessage("❌ Nation not found.").queue();
                return;
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(mcColorToRgb(nation.getColor1()));
            eb.setTitle("🏛 " + nation.getName());
            String presName = getCharacterName(nation.getPresidentUUID());
            eb.addField("President", presName != null ? presName : "None", true);
            if (nation.getVicePresidentUUID() != null) {
                String vpName = getCharacterName(nation.getVicePresidentUUID());
                eb.addField("Vice President", vpName != null ? vpName : "Vacant", true);
            }
            eb.addField("Members", String.valueOf(nation.getMemberUUIDs().size()), true);
            List<RegionData> regions = plugin.getNationManager().getRegionsByNation(nation.getNationId());
            eb.addField("Regions", String.valueOf(regions.size()), true);
            eb.addField("Tax Rates",
                    "Lower: " + String.format("%.1f", nation.getLowerClassTaxRate()) + "%\n"
                            + "Middle: " + String.format("%.1f", nation.getMiddleClassTaxRate()) + "%\n"
                            + "Upper: " + String.format("%.1f", nation.getUpperClassTaxRate()) + "%", true);
            List<WarData> wars = plugin.getWarManager().getWarsByNation(nation.getNationId());
            eb.addField("Active Wars", String.valueOf(wars.size()), true);
            if (!regions.isEmpty()) {
                StringBuilder regSb = new StringBuilder();
                int regShown = Math.min(regions.size(), 5);
                for (int i = 0; i < regShown; i++) {
                    regSb.append("• ").append(regions.get(i).getName())
                            .append(" (").append(regions.get(i).getClaimedChunks().size()).append(" chunks)\n");
                }
                if (regions.size() > 5) regSb.append("*...and ").append(regions.size() - 5).append(" more*");
                eb.addField("Region List", regSb.toString(), false);
            }
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }

    // ---- Leaderboard Command ----

    private void handleLeaderboard(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(0xFFD700);
            eb.setTitle("🏆 Server Leaderboard");
            List<CharacterData> allChars = new ArrayList<>(plugin.getCharacterManager().getAllCharacters());
            allChars.sort((a, b) -> {
                double ba = plugin.getEconomy().getBalance(plugin.getServer().getOfflinePlayer(a.getPlayerUuid()));
                double bb = plugin.getEconomy().getBalance(plugin.getServer().getOfflinePlayer(b.getPlayerUuid()));
                return Double.compare(bb, ba);
            });
            StringBuilder richest = new StringBuilder();
            int count = Math.min(10, allChars.size());
            for (int i = 0; i < count; i++) {
                CharacterData c = allChars.get(i);
                double bal = plugin.getEconomy().getBalance(plugin.getServer().getOfflinePlayer(c.getPlayerUuid()));
                String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "**" + (i + 1) + ".**";
                richest.append(medal).append(" ").append(c.getFirstName()).append(" ").append(c.getLastName())
                        .append(" — ").append(CurrencyUtil.symbol()).append(String.format("%.0f", bal)).append("\n");
            }
            eb.addField("💰 Richest Players", richest.length() > 0 ? richest.toString() : "No players", false);
            if (plugin.isDirtNationsEnabled()) {
                List<NationData> nations = new ArrayList<>(plugin.getNationManager().getAllNations());
                nations.sort((a, b) -> Integer.compare(b.getMemberUUIDs().size(), a.getMemberUUIDs().size()));
                StringBuilder natSb = new StringBuilder();
                int natCount = Math.min(5, nations.size());
                for (int i = 0; i < natCount; i++) {
                    NationData n = nations.get(i);
                    String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "**" + (i + 1) + ".**";
                    natSb.append(medal).append(" ").append(n.getName())
                            .append(" — ").append(n.getMemberUUIDs().size()).append(" members\n");
                }
                eb.addField("🏛 Top Nations", natSb.length() > 0 ? natSb.toString() : "No nations", false);
            }
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }

    // ---- Coordinates Command ----

    private void handleCoords(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CharacterData character = resolveCharacter(event.getUser().getId());
            if (character == null) {
                event.getHook().sendMessage("❌ No linked character found. Use `/d link` in-game first.").queue();
                return;
            }
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(character.getPlayerUuid());
            if (player == null || !player.isOnline()) {
                event.getHook().sendMessage("❌ You must be online in-game to use this command.").queue();
                return;
            }
            var loc = player.getLocation();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(0xE67E22);
            eb.setTitle("📍 Coordinates — " + character.getFirstName() + " " + character.getLastName());
            eb.addField("World", loc.getWorld().getName(), true);
            eb.addField("X", String.valueOf(loc.getBlockX()), true);
            eb.addField("Y", String.valueOf(loc.getBlockY()), true);
            eb.addField("Z", String.valueOf(loc.getBlockZ()), true);
            eb.addField("Direction", getCardinalDirection(loc.getYaw()), true);
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }

    private String getCardinalDirection(float yaw) {
        double rotation = (yaw % 360 + 360) % 360;
        if (rotation < 22.5 || rotation >= 337.5) return "South";
        if (rotation < 67.5) return "Southwest";
        if (rotation < 112.5) return "West";
        if (rotation < 157.5) return "Northwest";
        if (rotation < 202.5) return "North";
        if (rotation < 247.5) return "Northeast";
        if (rotation < 292.5) return "East";
        return "Southeast";
    }

    // ---- Chat Bridge ----

    public void broadcastToDiscord(String playerName, String message) {
        if (jda == null || !plugin.getSettings().isDiscordChatBridgeEnabled()) return;
        String channelId = plugin.getSettings().getDiscordChatBridgeChannelId();
        if (channelId == null || channelId.isEmpty()) return;
        var channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        channel.sendMessage("**" + playerName + "**: " + message).queue(null, e -> {});
    }

    // ---- Activity Feed ----

    public void postActivity(String title, String description, int color) {
        if (jda == null || !plugin.getSettings().isDiscordActivityFeedEnabled()) return;
        String channelId = plugin.getSettings().getDiscordActivityFeedChannelId();
        if (channelId == null || channelId.isEmpty()) return;
        var channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(color);
        eb.setTitle(title);
        eb.setDescription(description);
        eb.setTimestamp(Instant.now());
        channel.sendMessageEmbeds(eb.build()).queue(null, e -> {});
    }

    // ---- Auto-Link ----

    public void tryAutoLink(UUID playerUuid, String playerName) {
        if (jda == null || !plugin.getSettings().isDiscordAutoLinkEnabled()) return;
        String guildId = plugin.getSettings().getDiscordGuildId();
        if (guildId == null || guildId.isEmpty()) return;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        guild.loadMembers().onSuccess(members -> {
            String lowerName = playerName.toLowerCase();
            for (Member member : members) {
                if (member.getUser().isBot()) continue;
                CharacterData existing = resolveCharacter(member.getId());
                if (existing != null) continue;

                String username = member.getUser().getName().toLowerCase();
                String nickname = member.getNickname() != null ? member.getNickname().toLowerCase() : "";
                String effectiveName = member.getEffectiveName().toLowerCase();

                if (username.equals(lowerName) || effectiveName.equals(lowerName) || nickname.equals(lowerName)) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        CharacterData data = plugin.getCharacterManager().getCharacter(playerUuid);
                        if (data != null && data.getDiscordId() == null) {
                            data.setDiscordId(member.getId());
                            plugin.getCharacterManager().saveCharacter(data);
                            org.bukkit.entity.Player p = plugin.getServer().getPlayer(playerUuid);
                            if (p != null) {
                                p.sendMessage("§a§l[Discord] §aYour account was automatically linked to Discord user §f"
                                        + member.getUser().getName() + "§a!");
                            }
                            plugin.getLogger().info("[Discord] Auto-linked " + playerName
                                    + " to Discord user " + member.getUser().getName());
                            postActivity("🔗 Auto-Link", data.getFirstName() + " " + data.getLastName()
                                    + " was automatically linked to Discord.", 0x2ECC71);
                        }
                    });
                    return;
                }
            }
        });
    }

    public void sendLifeTokenLink(UUID playerUuid, String characterName, String link) {
        com.dirt.data.CharacterData data = plugin.getCharacterManager().getCharacter(playerUuid);
        if (data == null || data.getDiscordId() == null || jda == null) return;
        jda.retrieveUserById(data.getDiscordId()).queue(user -> {
            if (user == null) return;
            user.openPrivateChannel().queue(channel -> {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("☠ Life Token Store")
                        .setDescription("Your character **" + characterName + "** has died!\n\nGet life tokens to revive:")
                        .addField("🔗 Store Link", link, false)
                        .setColor(new java.awt.Color(255, 170, 0))
                        .setFooter("DirtLife • Get tokens, save lives!");
                channel.sendMessageEmbeds(embed.build()).queue(null, t -> {});
            }, t -> {});
        }, t -> {});
    }
}