package com.mteco.inventory.impl.contract;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.ContractData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.inventory.impl.nation.NationCharacterSelectGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.stream.Collectors;

public class ContractSignOptionsGUI extends InventoryGUI {
    private final MTeco plugin;
    private final ContractData draft;

    public ContractSignOptionsGUI(MTeco plugin, ContractData draft) {
        this.plugin = plugin;
        this.draft = draft;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Sign Contract");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.YELLOW_STAINED_GLASS_PANE);

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7eContract Ready",
                        "\u00a77Body: \u00a7f" + draft.getBody().length() + " chars",
                        "\u00a77Choose how to sign."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aSign it Myself",
                        "\u00a77You will be the first signer.",
                        "\u00a77Then send to another player."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(
                            new ContractBehalfOfGUI(plugin, draft, true), p);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7eSend to Someone Else",
                        "\u00a77Both signers will be other",
                        "\u00a77characters, not you."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    List<CharacterData> living = plugin.getCharacterManager().getAllCharacters().stream()
                            .filter(CharacterData::isAlive)
                            .filter(c -> !c.getPlayerUuid().equals(p.getUniqueId()))
                            .collect(Collectors.toList());
                    plugin.getGUIManager().openGUI(
                            new NationCharacterSelectGUI(plugin, "\u00a7aSelect First Signer", living,
                                    firstSigner -> selectSecondSigner(p, firstSigner, living),
                                    new ContractSignOptionsGUI(plugin, draft), 0), p);
                })
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new ContractCreationGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void selectSecondSigner(Player creator, CharacterData firstSigner, List<CharacterData> allLiving) {
        List<CharacterData> remaining = allLiving.stream()
                .filter(c -> !c.getPlayerUuid().equals(firstSigner.getPlayerUuid()))
                .collect(Collectors.toList());

        plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getGUIManager().openGUI(
                        new NationCharacterSelectGUI(plugin, "\u00a7aSelect Second Signer", remaining,
                                secondSigner -> sendToBothSigners(creator, firstSigner, secondSigner),
                                new ContractSignOptionsGUI(plugin, draft), 0), creator));
    }

    private void sendToBothSigners(Player creator, CharacterData firstSigner, CharacterData secondSigner) {
        draft.setStatus("PENDING_SIGNATURE");
        draft.setPendingRecipientUuid(firstSigner.getPlayerUuid());
        draft.setSecondRecipientUuid(secondSigner.getPlayerUuid());

        plugin.getContractManager().saveContract(draft);
        plugin.getContractManager().removeDraft(creator.getUniqueId());

        com.mteco.data.MailItem mail = new com.mteco.data.MailItem();
        mail.setId(java.util.UUID.randomUUID().toString());
        mail.setType("CONTRACT_SIGN_REQUEST");
        mail.setFromPlayerUuid(creator.getUniqueId());
        mail.setTimestamp(System.currentTimeMillis());
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("contractId", draft.getContractId().toString());
        data.put("fromName", draft.getCreatorCharacterName());
        mail.setData(data);
        plugin.getCharacterManager().addMailItem(firstSigner.getPlayerUuid(), mail);

        Player firstPlayer = Bukkit.getPlayer(firstSigner.getPlayerUuid());
        if (firstPlayer != null) {
            firstPlayer.sendMessage("\u00a7eYou have received a contract to sign! Check your mailbox.");
        }

        creator.sendMessage("\u00a7aContract sent to \u00a7e" + firstSigner.getFirstName() + " " + firstSigner.getLastName()
                + "\u00a7a for signing. After they sign, it will be sent to \u00a7e"
                + secondSigner.getFirstName() + " " + secondSigner.getLastName() + "\u00a7a.");
    }
}