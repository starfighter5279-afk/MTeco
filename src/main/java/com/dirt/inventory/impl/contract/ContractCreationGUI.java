package com.dirt.inventory.impl.contract;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.ContractData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.LText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContractCreationGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private ContractData specificDraft;

    public ContractCreationGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    public ContractCreationGUI(DirtEconomy plugin, java.util.UUID playerUuid) {
        this.plugin = plugin;
    }

    public ContractCreationGUI(DirtEconomy plugin, ContractData draft) {
        this.plugin = plugin;
        this.specificDraft = draft;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Create Contract");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.YELLOW_STAINED_GLASS_PANE);

        ContractData draft;
        if (specificDraft != null) {
            draft = specificDraft;
            plugin.getContractManager().setActiveDraft(player.getUniqueId(), draft);
        } else {
            draft = plugin.getContractManager().getOrCreateDraft(player.getUniqueId());
        }

        CharacterData ch = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        if (ch != null) {
            draft.setCreatorCharacterName(ch.getFirstName() + " " + ch.getLastName());
        }

        addButton(4, new InventoryButton()
                .creator(p -> {
                    List<String> lore = new ArrayList<>();
                    lore.add("\u00a77Author: \u00a7f" + draft.getCreatorCharacterName());
                    if (draft.getBody().isEmpty()) {
                        lore.add("\u00a77Body: \u00a7cNot set");
                    } else {
                        lore.add("\u00a77Body: \u00a7a" + draft.getBody().length() + " chars");
                    }
                    if (draft.getDurationMinecraftDays() > 0) {
                        lore.add("\u00a77Duration: \u00a7f" + draft.getDurationMinecraftDays() + " MC days");
                    } else {
                        lore.add("\u00a77Duration: \u00a7fIndefinite");
                    }
                    lore.add("");
                    lore.add("\u00a7eClick to view all drafts");
                    return ItemUtil.buildItem(XMaterial.PAPER, "\u00a7eContract Draft", lore.toArray(new String[0]));
                })
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new ContractDraftsGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7eEdit Body",
                        "\u00a77Type or paste the contract text.",
                        "\u00a77Use | for line breaks.",
                        "\u00a77You can also paste a link (PDF, TXT, etc.).",
                        draft.getBody().isEmpty() ? "\u00a7cNo body set yet." : "\u00a7aCurrent: " + draft.getBody().length() + " chars"))
                .consumer(e -> editBody(draft, (Player) e.getWhoClicked()))
        );

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.CLOCK, "\u00a7eSet Duration",
                        "\u00a77Set how many Minecraft days this",
                        "\u00a77contract will last. 0 = indefinite.",
                        "\u00a77Current: \u00a7f" + (draft.getDurationMinecraftDays() > 0 ?
                                draft.getDurationMinecraftDays() + " MC days" : "Indefinite")))
                .consumer(e -> setDuration(draft, (Player) e.getWhoClicked()))
        );

        addButton(14, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.FEATHER, "\u00a7aSign & Send",
                        "\u00a77Finalize and sign the contract.",
                        draft.getBody().isEmpty() ? "\u00a7c\u26a0 Must set body first!" : "\u00a7aReady to sign!"))
                .consumer(e -> {
                    if (draft.getBody().isEmpty()) {
                        ((Player) e.getWhoClicked()).sendMessage("\u00a7cYou must write the contract body first.");
                        return;
                    }
                    plugin.getGUIManager().openGUI(
                            new ContractSignOptionsGUI(plugin, draft), (Player) e.getWhoClicked());
                })
        );

        addButton(16, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eSet Title",
                        "\u00a77Give your contract a title.",
                        draft.getTitle().isEmpty() ? "\u00a7cNo title set." : "\u00a77Current: \u00a7f" + draft.getTitle()))
                .consumer(e -> setTitle(draft, (Player) e.getWhoClicked()))
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cCancel"))
                .consumer(e -> {
                    Player clicker = (Player) e.getWhoClicked();
                    if (draft.getBody().isEmpty() && draft.getDurationMinecraftDays() == 0) {
                        plugin.getContractManager().deleteDraft(draft.getContractId());
                    }
                    plugin.getContractManager().removeDraft(clicker.getUniqueId());
                    clicker.closeInventory();
                    clicker.sendMessage("\u00a7cContract creation cancelled.");
                })
        );

        super.decorate(player);
    }

    private void editBody(ContractData draft, Player player) {
        if (!draft.getBody().isEmpty()) {
            plugin.getGUIManager().openGUI(new ContractEditBodyOptionsGUI(plugin, draft), player);
            return;
        }
        promptBodyInput(plugin, draft, player, false);
    }

    static void promptBodyInput(DirtEconomy plugin, ContractData draft, Player player, boolean append) {
        player.closeInventory();
        String prompt = append
                ? "\u00a7eType or paste additional text to append. Use \u00a7b|\u00a7e for line breaks.\n\u00a77You can also paste a link to any document (PDF, TXT, etc.)."
                : "\u00a7eType or paste the contract body. Use \u00a7b|\u00a7e for line breaks.\n\u00a77You can also paste a link to any document (PDF, TXT, etc.).";
        plugin.getChatInputManager().requestInput(player, prompt, input -> {
            LText.handleInput(plugin, player, input, text -> {
                String newBody = append ? draft.getBody() + (draft.getBody().isEmpty() ? "" : "|") + text : text;
                draft.setBody(newBody);
                plugin.getContractManager().saveDraft(draft);
                player.sendMessage("\u00a7aContract body " + (append ? "updated" : "set") + "! (" + newBody.length() + " characters)");
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> plugin.getGUIManager().openGUI(new ContractCreationGUI(plugin, draft), player));
            }, () -> plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.getGUIManager().openGUI(new ContractCreationGUI(plugin, draft), player)));
        });
    }

    private void setDuration(ContractData draft, Player player) {
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter the number of Minecraft days (0 = indefinite):", input -> {
                    int days;
                    try { days = Integer.parseInt(input.trim()); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid number."); return;
                    }
                    if (days < 0) { player.sendMessage("\u00a7cDuration cannot be negative."); return; }
                    draft.setDurationMinecraftDays(days);
                    plugin.getContractManager().saveDraft(draft);
                    player.sendMessage("\u00a7aDuration set to " + (days > 0 ? days + " Minecraft days." : "indefinite."));
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new ContractCreationGUI(plugin, draft), player));
                });
    }

    private void setTitle(ContractData draft, Player player) {
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter a title for your contract:", input -> {
                    String title = input.trim();
                    if (title.length() > 64) {
                        title = title.substring(0, 64);
                        player.sendMessage("\u00a7eTitle trimmed to 64 characters.");
                    }
                    draft.setTitle(title);
                    plugin.getContractManager().saveDraft(draft);
                    player.sendMessage("\u00a7aContract title set to: \u00a7f" + title);
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new ContractCreationGUI(plugin, draft), player));
                });
    }
}