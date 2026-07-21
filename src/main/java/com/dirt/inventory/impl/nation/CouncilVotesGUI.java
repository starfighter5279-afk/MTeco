package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.CouncilVote;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class CouncilVotesGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final int page;

    public CouncilVotesGUI(DirtEconomy plugin, UUID nationId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Council Votes");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<CouncilVote> votes = plugin.getCouncilVoteManager().getVotesForNation(nationId);
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, votes.size());

        for (int i = start; i < end; i++) {
            CouncilVote vote = votes.get(i);
            int slot = i - start;

            boolean hasVoted = vote.getVotes().containsKey(player.getUniqueId());
            Boolean myVote = vote.getVotes().get(player.getUniqueId());
            boolean isEligible = vote.getEligibleVoters().contains(player.getUniqueId());

            CharacterData c = plugin.getCharacterManager().getCharacter(vote.getInitiatorUUID());
            String initiatorName = c != null ? c.getFirstName() + " " + c.getLastName() : "Unknown";

            long remaining = (vote.getExpiryTimestamp() - System.currentTimeMillis()) / 1000;
            String timeStr = remaining > 0 ? remaining + "s" : "Expiring...";

            String voteStatus;
            if (!isEligible) {
                voteStatus = "\u00a77You are not eligible to vote.";
            } else if (hasVoted) {
                voteStatus = myVote ? "\u00a7aYou voted: PASS" : "\u00a7cYou voted: DENY";
            } else {
                voteStatus = "\u00a7e\u00a7lLeft-click: Pass \u00a78| \u00a7c\u00a7lRight-click: Deny";
            }

            int passCount = 0, denyCount = 0;
            for (Boolean v : vote.getVotes().values()) {
                if (v) passCount++; else denyCount++;
            }
            int pending = vote.getEligibleVoters().size() - vote.getVotes().size();

            XMaterial icon;
            if (!isEligible) {
                icon = XMaterial.GRAY_WOOL;
            } else if (hasVoted) {
                icon = myVote ? XMaterial.LIME_WOOL : XMaterial.RED_WOOL;
            } else {
                icon = XMaterial.YELLOW_WOOL;
            }

            final int fp = passCount;
            final int fd = denyCount;
            final int fpend = pending;

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(icon, "\u00a76" + vote.getDescription(),
                            "\u00a77Proposed by: \u00a7e" + initiatorName,
                            "\u00a77Permission: \u00a7f" + vote.getPermission().getDisplayName(),
                            "\u00a77Votes: \u00a7a" + fp + " Pass \u00a78| \u00a7c" + fd + " Deny \u00a78| \u00a77" + fpend + " Pending",
                            "\u00a77Expires in: \u00a7f" + timeStr,
                            "",
                            voteStatus))
                    .consumer(e -> {
                        if (!isEligible || hasVoted) return;
                        boolean pass = e.isLeftClick();
                        plugin.getCouncilVoteManager().castVote(vote.getVoteId(), player.getUniqueId(), pass);
                        player.sendMessage(pass ? "\u00a7aVote cast: PASS" : "\u00a7cVote cast: DENY");
                        plugin.getGUIManager().openGUI(new CouncilVotesGUI(plugin, nationId, page), player);
                    })
            );
        }

        if (page > 0) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CouncilVotesGUI(plugin, nationId, page - 1), (Player) e.getWhoClicked()))
            );
        }
        if (end < votes.size()) {
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CouncilVotesGUI(plugin, nationId, page + 1), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CustomRoleDashboardGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}