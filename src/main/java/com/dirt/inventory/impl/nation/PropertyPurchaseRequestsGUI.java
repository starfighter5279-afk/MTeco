package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.NationData;
import com.dirt.data.PropertyData;
import com.dirt.data.PropertyPurchaseRequest;
import com.dirt.data.RegionData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class PropertyPurchaseRequestsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID regionId;

    public PropertyPurchaseRequestsGUI(DirtEconomy plugin, UUID regionId) {
        this.plugin = plugin;
        this.regionId = regionId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§ePurchase Requests");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<PropertyPurchaseRequest> requests = plugin.getNationManager().getPurchaseRequestsByRegion(regionId);

        for (int i = 0; i < requests.size() && i < 45; i++) {
            PropertyPurchaseRequest req = requests.get(i);
            CharacterData requester = plugin.getCharacterManager().getCharacter(req.getRequesterUUID());
            String requesterName = requester != null ? requester.getFirstName() + " " + requester.getLastName() : "Unknown";
            int slot = i * 2;
            if (slot >= 44) break;

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PAPER,
                            "§ePurchase Request",
                            "§7From: §f" + requesterName,
                            "§7Chunks: §f" + req.getRequestedChunks().size(),
                            "§7Price: §a" + CurrencyUtil.symbol() + String.format("%.2f", req.getTotalPrice()),
                            "§7Approved: §f" + (req.isGovernorApproved() || req.isTreasurerApproved() ? "§aYes" : "§cNo")))
                    .consumer(e -> {})
            );

            final PropertyPurchaseRequest finalReq = req;
            addButton(slot + 1, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "§aApprove"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        approveRequest(p, finalReq);
                    })
            );
        }

        addButton(49, new InventoryButton()
                .creator(pa -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernorPropertiesGUI(plugin, regionId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void approveRequest(Player approver, PropertyPurchaseRequest req) {
        Player requester = Bukkit.getPlayer(req.getRequesterUUID());
        if (!plugin.getEconomy().has(Bukkit.getOfflinePlayer(req.getRequesterUUID()), req.getTotalPrice())) {
            approver.sendMessage("§cThe requester cannot afford this property.");
            plugin.getNationManager().deletePurchaseRequest(req.getRequestId());
            plugin.getGUIManager().openGUI(new PropertyPurchaseRequestsGUI(plugin, regionId), approver);
            return;
        }

        plugin.getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(req.getRequesterUUID()), req.getTotalPrice());
        RegionData region = plugin.getNationManager().loadRegion(regionId);
        NationData nation = region != null ? plugin.getNationManager().loadNation(region.getNationId()) : null;
        if (nation != null) plugin.getNationManager().depositToTreasury(nation.getName(), req.getTotalPrice());

        PropertyData property = plugin.getNationManager().createProperty(regionId, req.getRequesterUUID(), req.getRequestedChunks(), req.getTotalPrice());
        if (req.getPropertyName() != null && !req.getPropertyName().isEmpty()) {
            property.setName(req.getPropertyName());
            plugin.getNationManager().saveProperty(property);
        }
        plugin.getNationManager().deletePurchaseRequest(req.getRequestId());

        approver.sendMessage("§aProperty purchase approved. Property created with §e" + property.getChunks().size() + "§a chunks.");
        if (requester != null) {
            requester.sendMessage("§aYour property purchase has been approved! You now own a §e" + property.getChunks().size() + "§a-chunk property.");
        }

        plugin.getGUIManager().openGUI(new PropertyPurchaseRequestsGUI(plugin, regionId), approver);
    }
}