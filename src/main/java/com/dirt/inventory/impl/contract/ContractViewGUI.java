package com.dirt.inventory.impl.contract;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.ContractData;
import com.dirt.data.ContractSignature;
import com.dirt.data.MailItem;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.impl.MailGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ContractViewGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID contractId;
    private final String mailIdToRemove;

    public ContractViewGUI(DirtEconomy plugin, UUID contractId) {
        this.plugin = plugin;
        this.contractId = contractId;
        this.mailIdToRemove = null;
    }

    public ContractViewGUI(DirtEconomy plugin, UUID contractId, String mailIdToRemove) {
        this.plugin = plugin;
        this.contractId = contractId;
        this.mailIdToRemove = mailIdToRemove;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 9, "\u00a76Contract Actions");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(9, XMaterial.YELLOW_STAINED_GLASS_PANE);

        ContractData contract = plugin.getContractManager().loadContract(contractId);
        if (contract == null) {
            addButton(4, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cContract not found."))
                    .consumer(e -> {}));
            super.decorate(player);
            return;
        }

        addButton(0, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITTEN_BOOK, "\u00a7eRead Contract",
                        "\u00a77Click to open the contract",
                        "\u00a77as a readable book."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    openContractBook(p, contract);
                })
        );

        List<String> sigLore = new ArrayList<>();
        sigLore.add("\u00a77Created: \u00a7f" + new SimpleDateFormat("MM/dd/yyyy").format(new Date(contract.getCreatedAt())));
        sigLore.add("\u00a77Status: \u00a7f" + contract.getStatus());
        if (contract.getDurationMinecraftDays() > 0) {
            sigLore.add("\u00a77Duration: \u00a7f" + contract.getDurationMinecraftDays() + " MC days");
        }
        if (!contract.getTitle().isEmpty()) {
            sigLore.add("\u00a77Title: \u00a7f" + contract.getTitle());
        }
        sigLore.add("");
        for (ContractSignature sig : contract.getSignatures()) {
            String behalf = sig.getOnBehalfOf() != null ? " \u00a77(for \u00a7f" + sig.getOnBehalfOf() + "\u00a77)" : "";
            sigLore.add("\u00a7a\u2713 \u00a7f" + sig.getCharacterName() + behalf);
        }

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "\u00a7eSignatures & Info", sigLore.toArray(new String[0])))
                .consumer(e -> {})
        );

        boolean isPending = "PENDING_SIGNATURE".equals(contract.getStatus())
                && player.getUniqueId().equals(contract.getPendingRecipientUuid());

        if (isPending) {
            addButton(2, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aSign Contract",
                            "\u00a77Add your signature to this contract."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new ContractBehalfOfGUI(plugin, contract, false), (Player) e.getWhoClicked()))
            );

            addButton(6, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDeny Contract",
                            "\u00a77Refuse to sign this contract."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        contract.setStatus("DENIED");
                        plugin.getContractManager().saveContract(contract);
                        if (mailIdToRemove != null) {
                            plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mailIdToRemove);
                        }
                        p.sendMessage("\u00a7cYou denied the contract.");
                        notifyCreator(contract, "denied");
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                    })
            );

            addButton(7, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ORANGE_WOOL, "\u00a76Request Modification",
                            "\u00a77Ask the creator to modify the contract."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        contract.setStatus("MODIFICATION_REQUESTED");
                        plugin.getContractManager().saveContract(contract);

                        MailItem mail = new MailItem();
                        mail.setId(UUID.randomUUID().toString());
                        mail.setType("CONTRACT_MODIFICATION");
                        mail.setFromPlayerUuid(p.getUniqueId());
                        mail.setTimestamp(System.currentTimeMillis());
                        Map<String, String> data = new HashMap<>();
                        data.put("contractId", contract.getContractId().toString());
                        com.dirt.data.CharacterData ch = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                        data.put("fromName", ch != null ? ch.getFirstName() + " " + ch.getLastName() : p.getName());
                        mail.setData(data);
                        plugin.getCharacterManager().addMailItem(contract.getCreatorPlayerUuid(), mail);

                        p.sendMessage("\u00a7eModification request sent to the contract creator.");
                        if (mailIdToRemove != null) {
                            plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mailIdToRemove);
                        }
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                    })
            );
        }

        addButton(8, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void openContractBook(Player player, ContractData contract) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        String title = contract.getTitle().isEmpty() ? "Contract" : contract.getTitle();
        meta.title(ItemUtil.legacyComponent(title));
        String author = contract.getCreatorCharacterName() != null ? contract.getCreatorCharacterName() : "Unknown";
        meta.author(ItemUtil.legacyComponent(author));

        String bodyText = contract.getBody().replace("|", "\n");
        List<String> pages = paginateText(bodyText);

        String headerPage = "\u00a70\u00a7l" + title + "\n\n"
                + "\u00a78Author: \u00a70" + author + "\n"
                + "\u00a78Date: \u00a70" + new SimpleDateFormat("MM/dd/yyyy").format(new Date(contract.getCreatedAt())) + "\n"
                + "\u00a78Status: \u00a70" + contract.getStatus() + "\n";
        if (contract.getDurationMinecraftDays() > 0) {
            headerPage += "\u00a78Duration: \u00a70" + contract.getDurationMinecraftDays() + " MC days\n";
        }
        headerPage += "\n\u00a78--- Turn page to read ---";
        meta.addPages(ItemUtil.legacyComponent(headerPage));

        for (String page : pages) {
            meta.addPages(ItemUtil.legacyComponent("\u00a70" + page));
        }

        if (!contract.getSignatures().isEmpty()) {
            StringBuilder sigPage = new StringBuilder("\u00a70\u00a7lSignatures\n\n");
            for (ContractSignature sig : contract.getSignatures()) {
                sigPage.append("\u00a72\u2713 \u00a70").append(sig.getCharacterName());
                if (sig.getOnBehalfOf() != null) {
                    sigPage.append("\n  \u00a78(for ").append(sig.getOnBehalfOf()).append(")");
                }
                sigPage.append("\n\u00a78Code: ").append(sig.getCharacterCode()).append("\n\n");
            }
            meta.addPages(ItemUtil.legacyComponent(sigPage.toString()));
        }

        book.setItemMeta(meta);
        player.openBook(book);
    }

    private List<String> paginateText(String text) {
        List<String> pages = new ArrayList<>();
        int maxCharsPerPage = 256;
        int maxLinesPerPage = 14;

        String[] paragraphs = text.split("\n");
        StringBuilder currentPage = new StringBuilder();
        int lineCount = 0;

        for (String paragraph : paragraphs) {
            List<String> wrapped = wordWrap(paragraph, 19);
            for (String line : wrapped) {
                if (lineCount >= maxLinesPerPage || currentPage.length() + line.length() + 1 > maxCharsPerPage) {
                    if (currentPage.length() > 0) {
                        pages.add(currentPage.toString());
                        currentPage = new StringBuilder();
                        lineCount = 0;
                    }
                }
                if (currentPage.length() > 0) currentPage.append("\n");
                currentPage.append(line);
                lineCount++;
            }
        }

        if (currentPage.length() > 0) {
            pages.add(currentPage.toString());
        }

        if (pages.isEmpty()) {
            pages.add("(Empty contract)");
        }

        return pages;
    }

    private List<String> wordWrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }
        while (text.length() > maxWidth) {
            int split = text.lastIndexOf(' ', maxWidth);
            if (split <= 0) split = maxWidth;
            lines.add(text.substring(0, split));
            text = text.substring(split).trim();
        }
        if (!text.isEmpty()) lines.add(text);
        return lines;
    }

    private void notifyCreator(ContractData contract, String action) {
        Player creator = Bukkit.getPlayer(contract.getCreatorPlayerUuid());
        if (creator != null) {
            creator.sendMessage("\u00a7eYour contract has been " + action + ".");
        }
    }
}