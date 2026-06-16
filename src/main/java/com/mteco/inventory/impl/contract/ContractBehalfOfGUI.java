package com.mteco.inventory.impl.contract;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.CharacterData;
import com.mteco.data.ContractData;
import com.mteco.data.ContractSignature;
import com.mteco.data.MailItem;
import com.mteco.data.NationData;
import com.mteco.data.NationPermission;
import com.mteco.data.BusinessPermission;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.inventory.impl.nation.NationCharacterSelectGUI;
import com.mteco.managers.ContractManager;
import com.mteco.util.ItemUtil;
import com.mteco.util.NationPermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ContractBehalfOfGUI extends InventoryGUI {
    private final MTeco plugin;
    private final ContractData contract;
    private final boolean creatorSigning;

    public ContractBehalfOfGUI(MTeco plugin, ContractData contract, boolean creatorSigning) {
        this.plugin = plugin;
        this.contract = contract;
        this.creatorSigning = creatorSigning;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Signing on Behalf of...");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.YELLOW_STAINED_GLASS_PANE);

        CharacterData ch = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        String charName = ch != null ? ch.getFirstName() + " " + ch.getLastName() : player.getName();

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7aPersonal",
                        "\u00a77Sign as yourself: \u00a7f" + charName))
                .consumer(e -> signContract(player, "PERSONAL", null, null))
        );

        int slot = 9;

        if (plugin.isMTBusinessEnabled()) {
            List<BusinessData> owned = plugin.getBusinessManager().getBusinessesByOwner(player.getUniqueId());
            List<BusinessData> managed = plugin.getBusinessManager().getBusinessesByEmployee(player.getUniqueId()).stream()
                    .filter(b -> b.hasPermission(player.getUniqueId(), BusinessPermission.MANAGE_CONTRACTS))
                    .collect(Collectors.toList());
            List<BusinessData> allBiz = new ArrayList<>(owned);
            for (BusinessData b : managed) {
                if (!owned.contains(b)) allBiz.add(b);
            }
            for (BusinessData biz : allBiz) {
                if (slot >= 36) break;
                addButton(slot, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a76" + biz.getName(),
                                "\u00a77Sign on behalf of this business."))
                        .consumer(e -> signContract(player, "BUSINESS", biz.getBusinessId(), biz.getName()))
                );
                slot++;
            }
        }

        if (plugin.isMTNationsEnabled()) {
            NationData nation = plugin.getNationManager().getNationByMember(player.getUniqueId());
            if (nation != null && NationPermissionUtil.hasPermission(plugin, nation, player.getUniqueId(), NationPermission.MANAGE_CONTRACTS)) {
                addButton(slot < 36 ? slot : 36, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.GOLDEN_HELMET,
                                nation.getColor1() + nation.getName(),
                                "\u00a77Sign on behalf of this nation."))
                        .consumer(e -> signContract(player, "NATION", nation.getNationId(), nation.getName()))
                );
            }
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> {
                    if (creatorSigning) {
                        plugin.getGUIManager().openGUI(new ContractSignOptionsGUI(plugin, contract), (Player) e.getWhoClicked());
                    } else {
                        plugin.getGUIManager().openGUI(new ContractViewGUI(plugin, contract.getContractId()), (Player) e.getWhoClicked());
                    }
                })
        );

        super.decorate(player);
    }

    private void signContract(Player player, String type, UUID entityId, String entityName) {
        CharacterData ch = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        if (ch == null) {
            player.sendMessage("\u00a7cYou need a character to sign contracts.");
            return;
        }

        ContractSignature sig = new ContractSignature();
        sig.setPlayerUuid(player.getUniqueId());
        sig.setCharacterName(ch.getFirstName() + " " + ch.getLastName());
        sig.setCharacterCode(ContractManager.getCharacterCode(ch));
        sig.setOnBehalfOfType(type);
        sig.setSignedAt(System.currentTimeMillis());
        if (entityId != null) {
            sig.setOnBehalfOfId(entityId);
            sig.setOnBehalfOf(entityName);
        }

        contract.getSignatures().add(sig);

        if (!contract.getLinkedPlayerUuids().contains(player.getUniqueId())) {
            contract.getLinkedPlayerUuids().add(player.getUniqueId());
        }
        if ("BUSINESS".equals(type) && entityId != null && !contract.getLinkedBusinessIds().contains(entityId)) {
            contract.getLinkedBusinessIds().add(entityId);
        }
        if ("NATION".equals(type) && entityId != null && !contract.getLinkedNationIds().contains(entityId)) {
            contract.getLinkedNationIds().add(entityId);
        }

        if (creatorSigning) {
            player.sendMessage("\u00a7aYou signed the contract! Now select who to send it to.");
            List<CharacterData> living = plugin.getCharacterManager().getAllCharacters().stream()
                    .filter(CharacterData::isAlive)
                    .filter(c -> !c.getPlayerUuid().equals(player.getUniqueId()))
                    .collect(Collectors.toList());

            contract.setStatus("PENDING_SIGNATURE");
            plugin.getContractManager().saveContract(contract);
            plugin.getContractManager().removeDraft(player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () ->
                    plugin.getGUIManager().openGUI(
                            new NationCharacterSelectGUI(plugin, "\u00a7aSend Contract To", living,
                                    target -> sendContractToRecipient(player, target),
                                    null, 0), player));
        } else {
            handleCounterSign(player);
        }
    }

    private void sendContractToRecipient(Player sender, CharacterData target) {
        contract.setPendingRecipientUuid(target.getPlayerUuid());
        plugin.getContractManager().saveContract(contract);

        MailItem mail = new MailItem();
        mail.setId(UUID.randomUUID().toString());
        mail.setType("CONTRACT_SIGN_REQUEST");
        mail.setFromPlayerUuid(sender.getUniqueId());
        mail.setTimestamp(System.currentTimeMillis());
        Map<String, String> data = new HashMap<>();
        data.put("contractId", contract.getContractId().toString());
        data.put("fromName", contract.getCreatorCharacterName());
        String preview = contract.getBody().length() > 30 ? contract.getBody().substring(0, 30) + "..." : contract.getBody();
        data.put("summary", preview);
        mail.setData(data);
        plugin.getCharacterManager().addMailItem(target.getPlayerUuid(), mail);

        Player targetPlayer = Bukkit.getPlayer(target.getPlayerUuid());
        if (targetPlayer != null) {
            targetPlayer.sendMessage("\u00a7eYou have received a contract to sign! Check your mailbox.");
        }

        sender.sendMessage("\u00a7aContract sent to \u00a7e" + target.getFirstName() + " " + target.getLastName() + "\u00a7a!");
    }

    private void handleCounterSign(Player player) {
        if (contract.getSignatures().size() >= 2) {
            activateContract();
            player.sendMessage("\u00a7aContract fully signed and now active!");
            player.closeInventory();
            return;
        }

        if (contract.getSecondRecipientUuid() != null) {
            contract.setPendingRecipientUuid(contract.getSecondRecipientUuid());
            contract.setSecondRecipientUuid(null);
            plugin.getContractManager().saveContract(contract);

            MailItem mail = new MailItem();
            mail.setId(UUID.randomUUID().toString());
            mail.setType("CONTRACT_SIGN_REQUEST");
            mail.setFromPlayerUuid(contract.getCreatorPlayerUuid());
            mail.setTimestamp(System.currentTimeMillis());
            Map<String, String> data = new HashMap<>();
            data.put("contractId", contract.getContractId().toString());
            data.put("fromName", contract.getCreatorCharacterName());
            String preview = contract.getBody().length() > 30 ? contract.getBody().substring(0, 30) + "..." : contract.getBody();
            data.put("summary", preview);
            mail.setData(data);
            plugin.getCharacterManager().addMailItem(contract.getPendingRecipientUuid(), mail);

            Player nextPlayer = Bukkit.getPlayer(contract.getPendingRecipientUuid());
            if (nextPlayer != null) {
                nextPlayer.sendMessage("\u00a7eYou have received a contract to sign! Check your mailbox.");
            }

            player.sendMessage("\u00a7aYou signed! The contract has been sent to the next signer.");
            player.closeInventory();
        } else {
            activateContract();
            player.sendMessage("\u00a7aContract fully signed and now active!");
            player.closeInventory();
        }
    }

    private void activateContract() {
        contract.setStatus("ACTIVE");
        contract.setPendingRecipientUuid(null);
        if (contract.getDurationMinecraftDays() > 0) {
            long durationMs = (long) contract.getDurationMinecraftDays() * 20L * 60L * 1000L;
            contract.setExpiresAt(System.currentTimeMillis() + durationMs);
        }
        plugin.getContractManager().saveContract(contract);

        for (UUID linkedPlayer : contract.getLinkedPlayerUuids()) {
            Player p = Bukkit.getPlayer(linkedPlayer);
            if (p != null) {
                p.sendMessage("\u00a7aA contract you are part of is now active!");
            }
        }
    }
}