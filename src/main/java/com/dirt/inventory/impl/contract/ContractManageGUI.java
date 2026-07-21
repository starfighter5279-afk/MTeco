package com.dirt.inventory.impl.contract;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.ContractData;
import com.dirt.data.ContractSignature;
import com.dirt.data.MailItem;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ContractManageGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID contractId;
    private final InventoryGUI backGui;

    public ContractManageGUI(DirtEconomy plugin, UUID contractId, InventoryGUI backGui) {
        this.plugin = plugin;
        this.contractId = contractId;
        this.backGui = backGui;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 9, "\u00a76Contract Details");
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

        String displayTitle = contract.getTitle().isEmpty() ? "Contract" : contract.getTitle();

        addButton(1, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITTEN_BOOK, "\u00a7eRead Contract",
                        "\u00a77Title: \u00a7f" + displayTitle,
                        "\u00a77Author: \u00a7f" + contract.getCreatorCharacterName(),
                        "",
                        "\u00a7eClick to read the full contract."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    openContractBook(p, contract);
                })
        );

        List<String> infoLore = new ArrayList<>();
        infoLore.add("\u00a77Status: \u00a7f" + contract.getStatus());
        infoLore.add("\u00a77Created: \u00a7f" + new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date(contract.getCreatedAt())));
        if (contract.getDurationMinecraftDays() > 0) {
            infoLore.add("\u00a77Duration: \u00a7f" + contract.getDurationMinecraftDays() + " MC days");
            if (contract.getExpiresAt() > 0) {
                infoLore.add("\u00a77Expires: \u00a7f" + new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date(contract.getExpiresAt())));
            }
        }
        infoLore.add("");
        infoLore.add("\u00a7e--- Signatures ---");
        for (ContractSignature sig : contract.getSignatures()) {
            String behalf = sig.getOnBehalfOf() != null ? " (for " + sig.getOnBehalfOf() + ")" : "";
            infoLore.add("\u00a7a\u2713 " + sig.getCharacterName() + behalf);
            infoLore.add("  \u00a78Code: " + sig.getCharacterCode());
        }

        addButton(3, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "\u00a7eContract Info", infoLore.toArray(new String[0])))
                .consumer(e -> {})
        );

        if ("ACTIVE".equals(contract.getStatus())) {
            addButton(5, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cBreak Contract",
                            "\u00a77End this contract immediately.",
                            "\u00a77The other party will be notified."))
                    .consumer(e -> {
                        contract.setStatus("BROKEN");
                        plugin.getContractManager().saveContract(contract);
                        Player p = (Player) e.getWhoClicked();
                        p.sendMessage("\u00a7cContract broken.");
                        com.dirt.data.CharacterData myChar = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                        String myName = myChar != null ? myChar.getFirstName() + " " + myChar.getLastName() : p.getName();
                        for (UUID linked : contract.getLinkedPlayerUuids()) {
                            if (!linked.equals(p.getUniqueId())) {
                                Player other = Bukkit.getPlayer(linked);
                                if (other != null) {
                                    other.sendMessage("\u00a7cA contract you signed has been broken by " + myName + ".");
                                }
                                MailItem breakMail = new MailItem();
                                breakMail.setId(UUID.randomUUID().toString());
                                breakMail.setType("CONTRACT_BROKEN");
                                breakMail.setFromPlayerUuid(p.getUniqueId());
                                breakMail.setTimestamp(System.currentTimeMillis());
                                Map<String, String> mailData = new HashMap<>();
                                mailData.put("fromName", myName);
                                String preview = contract.getBody().length() > 30 ? contract.getBody().substring(0, 30) + "..." : contract.getBody();
                                mailData.put("summary", preview);
                                breakMail.setData(mailData);
                                plugin.getCharacterManager().addMailItem(linked, breakMail);
                            }
                        }
                        if (backGui != null) plugin.getGUIManager().openGUI(backGui, p);
                        else p.closeInventory();
                    })
            );

            double paperCost = plugin.getSettings().getPaperCopyCost();
            addButton(7, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7ePaper Copy",
                            "\u00a77Get a physical paper copy of this contract.",
                            "\u00a77Cost: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", paperCost)))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (!plugin.getEconomy().has(p, paperCost)) {
                            p.sendMessage("\u00a7cYou need " + CurrencyUtil.symbol() + String.format("%.2f", paperCost) + " for a paper copy.");
                            return;
                        }
                        plugin.getEconomy().withdrawPlayer(p, paperCost);
                        ItemStack paper = createPaperCopy(contract);
                        p.getInventory().addItem(paper);
                        p.sendMessage("\u00a7aPaper copy added to your inventory for \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", paperCost) + "\u00a7a.");
                    })
            );
        }

        addButton(8, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> {
                    if (backGui != null) plugin.getGUIManager().openGUI(backGui, (Player) e.getWhoClicked());
                    else ((Player) e.getWhoClicked()).closeInventory();
                })
        );

        super.decorate(player);
    }

    private void openContractBook(Player player, ContractData contract) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;

        String titleText = contract.getTitle().isEmpty() ? "Contract" : contract.getTitle();
        meta.setTitle(titleText);
        meta.setAuthor(contract.getCreatorCharacterName());

        String text = contract.getBody().replace("|", "\n");
        List<String> pages = paginateText(text);

        StringBuilder sigPage = new StringBuilder();
        sigPage.append("\u00a70\u00a7l--- Signatures ---\n\n");
        for (ContractSignature sig : contract.getSignatures()) {
            String behalf = sig.getOnBehalfOf() != null ? "\n  \u00a77(for " + sig.getOnBehalfOf() + ")" : "";
            sigPage.append("\u00a72\u2713 \u00a70").append(sig.getCharacterName()).append(behalf).append("\n");
            sigPage.append("  \u00a78Code: ").append(sig.getCharacterCode()).append("\n\n");
        }
        sigPage.append("\u00a77Status: \u00a70").append(contract.getStatus()).append("\n");
        sigPage.append("\u00a77Date: \u00a70").append(new SimpleDateFormat("MM/dd/yyyy").format(new Date(contract.getCreatedAt())));
        if (contract.getDurationMinecraftDays() > 0) {
            sigPage.append("\n\u00a77Duration: \u00a70").append(contract.getDurationMinecraftDays()).append(" MC days");
        }

        for (String page : pages) {
            meta.addPage(page);
        }
        meta.addPage(sigPage.toString());

        book.setItemMeta(meta);
        player.openBook(book);
    }

    private List<String> paginateText(String text) {
        List<String> pages = new ArrayList<>();
        int maxCharsPerPage = 256;
        int maxLinesPerPage = 14;

        StringBuilder currentPage = new StringBuilder();
        int lineCount = 0;

        for (String line : text.split("\n")) {
            String wrapped = wordWrap(line, 19);
            for (String wrapLine : wrapped.split("\n")) {
                if (lineCount >= maxLinesPerPage || currentPage.length() + wrapLine.length() + 1 > maxCharsPerPage) {
                    if (currentPage.length() > 0) {
                        pages.add("\u00a70" + currentPage.toString());
                        currentPage = new StringBuilder();
                        lineCount = 0;
                    }
                }
                if (currentPage.length() > 0) currentPage.append("\n");
                currentPage.append(wrapLine);
                lineCount++;
            }
        }
        if (currentPage.length() > 0) {
            pages.add("\u00a70" + currentPage.toString());
        }
        if (pages.isEmpty()) {
            pages.add("\u00a77(Empty contract)");
        }
        return pages;
    }

    private String wordWrap(String line, int maxWidth) {
        if (line.length() <= maxWidth) return line;
        StringBuilder result = new StringBuilder();
        while (line.length() > maxWidth) {
            int split = line.lastIndexOf(' ', maxWidth);
            if (split <= 0) split = maxWidth;
            result.append(line, 0, split).append("\n");
            line = line.substring(split).trim();
        }
        if (!line.isEmpty()) result.append(line);
        return result.toString();
    }

    private ItemStack createPaperCopy(ContractData contract) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return book;

        String titleText = contract.getTitle().isEmpty() ? "Contract Copy" : contract.getTitle();
        meta.setTitle(titleText);
        meta.setAuthor(contract.getCreatorCharacterName());

        String text = contract.getBody().replace("|", "\n");
        List<String> pages = paginateText(text);

        StringBuilder sigPage = new StringBuilder();
        sigPage.append("\u00a70\u00a7l--- Signatures ---\n\n");
        for (ContractSignature sig : contract.getSignatures()) {
            String behalf = sig.getOnBehalfOf() != null ? "\n  \u00a77(for " + sig.getOnBehalfOf() + ")" : "";
            sigPage.append("\u00a72\u2713 \u00a70").append(sig.getCharacterName()).append(behalf).append("\n");
            sigPage.append("  \u00a78Code: ").append(sig.getCharacterCode()).append("\n\n");
        }
        sigPage.append("\u00a77Created: \u00a70").append(new SimpleDateFormat("MM/dd/yyyy").format(new Date(contract.getCreatedAt())));

        for (String page : pages) {
            meta.addPage(page);
        }
        meta.addPage(sigPage.toString());

        book.setItemMeta(meta);
        return book;
    }
}