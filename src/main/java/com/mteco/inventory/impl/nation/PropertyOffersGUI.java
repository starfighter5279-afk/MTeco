package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.NationData;
import com.mteco.data.PropertyData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.CurrencyUtil;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PropertyOffersGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID propertyId;
    private final PropertyManageGUI.PropertyType type;
    private final UUID contextId;

    public PropertyOffersGUI(MTeco plugin, UUID propertyId, PropertyManageGUI.PropertyType type, UUID contextId) {
        this.plugin = plugin;
        this.propertyId = propertyId;
        this.type = type;
        this.contextId = contextId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eProperty Offers");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        PropertyData prop = plugin.getNationManager().loadProperty(propertyId);
        if (prop == null || prop.getOffers().isEmpty()) { super.decorate(player); return; }

        List<Map.Entry<UUID, Double>> offerList = new ArrayList<>(prop.getOffers().entrySet());
        for (int i = 0; i < offerList.size() && i < 18; i++) {
            Map.Entry<UUID, Double> entry = offerList.get(i);
            UUID buyerUUID = entry.getKey();
            double amount = entry.getValue();
            CharacterData buyerChar = plugin.getCharacterManager().getCharacter(buyerUUID);
            String buyerName = buyerChar != null ? buyerChar.getFirstName() + " " + buyerChar.getLastName() : Bukkit.getOfflinePlayer(buyerUUID).getName();
            if (buyerName == null) buyerName = "Unknown";
            final String finalBuyerName = buyerName;

            addButton(i, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD,
                            "\u00a7e" + finalBuyerName,
                            "\u00a77Offer: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", amount),
                            "\u00a7aClick to accept",
                            "\u00a7cShift-click to decline"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        PropertyData pr = plugin.getNationManager().loadProperty(propertyId);
                        if (pr == null) return;
                        if (e.isShiftClick()) {
                            pr.getOffers().remove(buyerUUID);
                            plugin.getNationManager().saveProperty(pr);
                            p.sendMessage("\u00a7cOffer from \u00a7e" + finalBuyerName + "\u00a7c declined.");
                            Player buyer = Bukkit.getPlayer(buyerUUID);
                            if (buyer != null) buyer.sendMessage("\u00a7cYour offer on a property was declined.");
                            plugin.getGUIManager().openGUI(new PropertyOffersGUI(plugin, propertyId, type, contextId), p);
                        } else {
                            acceptOffer(p, buyerUUID, amount, finalBuyerName);
                        }
                    })
            );
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new PropertyManageGUI(plugin, propertyId, type, contextId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void acceptOffer(Player owner, UUID buyerUUID, double amount, String buyerName) {
        PropertyData prop = plugin.getNationManager().loadProperty(propertyId);
        if (prop == null) return;

        org.bukkit.OfflinePlayer buyerOffline = Bukkit.getOfflinePlayer(buyerUUID);
        if (!plugin.getEconomy().has(buyerOffline, amount)) {
            owner.sendMessage("\u00a7c" + buyerName + " can no longer afford this offer.");
            prop.getOffers().remove(buyerUUID);
            plugin.getNationManager().saveProperty(prop);
            plugin.getGUIManager().openGUI(new PropertyOffersGUI(plugin, propertyId, type, contextId), owner);
            return;
        }

        plugin.getEconomy().withdrawPlayer(buyerOffline, amount);

        if (type == PropertyManageGUI.PropertyType.GOVERNMENT && contextId != null) {
            NationData nation = plugin.getNationManager().loadNation(contextId);
            if (nation != null) {
                plugin.getNationManager().depositToTreasury(nation.getName(), amount);
                nation.getGovernmentPropertyIds().remove(propertyId);
                plugin.getNationManager().saveNation(nation);
            }
        } else {
            plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(prop.getOwnerUUID()), amount);
        }

        String signLocKey = prop.getSaleSignLocation();
        prop.setForSale(false);
        prop.setSalePrice(0);
        prop.setSaleSignLocation(null);
        prop.setOwnerUUID(buyerUUID);
        prop.getOffers().clear();
        plugin.getNationManager().saveProperty(prop);

        if (signLocKey != null) removeSaleSign(signLocKey);

        owner.sendMessage("\u00a7aOffer accepted! Property sold to \u00a7e" + buyerName + "\u00a7a for \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a.");
        Player buyer = Bukkit.getPlayer(buyerUUID);
        if (buyer != null) buyer.sendMessage("\u00a7aYour offer of \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a was accepted! You now own the property.");

        owner.closeInventory();
    }

    private void removeSaleSign(String locKey) {
        try {
            String[] parts = locKey.split(",");
            if (parts.length < 4) return;
            org.bukkit.World world = Bukkit.getWorld(parts[0]);
            if (world == null) return;
            Block block = world.getBlockAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            if (block.getType().name().contains("SIGN")) block.setType(Material.AIR);
        } catch (Exception ignored) {}
    }
}