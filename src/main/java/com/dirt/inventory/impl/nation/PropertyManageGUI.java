package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessPropertyData;
import com.dirt.data.PropertyData;
import com.dirt.data.RegionData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class PropertyManageGUI extends InventoryGUI {
    public enum PropertyType { PERSONAL, GOVERNMENT, BUSINESS }

    private final DirtEconomy plugin;
    private final UUID propertyId;
    private final PropertyType type;
    private final UUID contextId;

    public PropertyManageGUI(DirtEconomy plugin, UUID propertyId, PropertyType type, UUID contextId) {
        this.plugin = plugin;
        this.propertyId = propertyId;
        this.type = type;
        this.contextId = contextId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Property Management");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        if (type == PropertyType.BUSINESS) {
            decorateBusiness(player);
        } else {
            decorateNation(player);
        }

        super.decorate(player);
    }

    private void decorateNation(Player player) {
        PropertyData prop = plugin.getNationManager().loadProperty(propertyId);
        if (prop == null) return;

        RegionData region = plugin.getNationManager().loadRegion(prop.getRegionId());
        String regionName = region != null ? region.getName() : "Unknown";
        String propName = prop.getName() != null && !prop.getName().isEmpty() ? prop.getName() : "Unnamed";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR,
                        "\u00a7a" + propName,
                        "\u00a77Region: \u00a7f" + regionName,
                        "\u00a77Chunks: \u00a7f" + prop.getChunks().size(),
                        "\u00a77For Sale: \u00a7f" + (prop.isForSale() ? "\u00a7aYes (" + CurrencyUtil.symbol() + String.format("%.2f", prop.getSalePrice()) + ")" : "\u00a7cNo")))
                .consumer(e -> {})
        );

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eRename Property",
                        "\u00a77Current: \u00a7f" + propName))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new property name:", input -> {
                        String name = input.trim();
                        if (name.isEmpty()) { p.sendMessage("\u00a7cName cannot be empty."); return; }
                        PropertyData pr = plugin.getNationManager().loadProperty(propertyId);
                        if (pr == null) return;
                        pr.setName(name);
                        plugin.getNationManager().saveProperty(pr);
                        p.sendMessage("\u00a7aProperty renamed to '\u00a76" + name + "\u00a7a'.");
                        plugin.getGUIManager().openGUI(new PropertyManageGUI(plugin, propertyId, type, contextId), p);
                    });
                })
        );

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "\u00a7eExtend Property",
                        "\u00a77Add more chunks to this property.",
                        "\u00a77Cost: \u00a7f" + CurrencyUtil.symbol() + String.format("%.2f", region != null ? region.getPropertyChunkRate() : 0) + " \u00a77per chunk"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChunkSelectionManager().startExtendProperty(p, propertyId,
                            type == PropertyType.GOVERNMENT, type == PropertyType.GOVERNMENT ? contextId : null);
                })
        );

        if (prop.isForSale()) {
            addButton(14, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cRemove From Sale",
                            prop.getSaleSignLocation() != null ? "\u00a77Sale sign is active." : "\u00a7eAwaiting sign placement."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        PropertyData pr = plugin.getNationManager().loadProperty(propertyId);
                        if (pr == null) return;
                        String signLoc = pr.getSaleSignLocation();
                        pr.setForSale(false);
                        pr.setSalePrice(0);
                        pr.setSaleSignLocation(null);
                        pr.getOffers().clear();
                        plugin.getNationManager().saveProperty(pr);
                        if (signLoc != null) removeSaleSign(signLoc);
                        p.sendMessage("\u00a7aProperty removed from sale.");
                        plugin.getGUIManager().openGUI(new PropertyManageGUI(plugin, propertyId, type, contextId), p);
                    })
            );
        } else {
            addButton(14, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7aSell Property",
                            "\u00a77Place a sign with '\u00a7fFor Sale\u00a77'",
                            "\u00a77on the property to activate."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getChatInputManager().requestInput(p, "\u00a7eEnter the sale price:", input -> {
                            try {
                                double price = Double.parseDouble(input);
                                if (price <= 0) { p.sendMessage("\u00a7cPrice must be positive."); return; }
                                PropertyData pr = plugin.getNationManager().loadProperty(propertyId);
                                if (pr == null) return;
                                pr.setForSale(true);
                                pr.setSalePrice(price);
                                pr.setSaleSignLocation(null);
                                plugin.getNationManager().saveProperty(pr);
                                p.sendMessage("\u00a7aProperty listed for \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", price) + "\u00a7a.");
                                p.sendMessage("\u00a7ePlace a sign on your property and write '\u00a76For Sale\u00a7e' to activate.");
                            } catch (NumberFormatException ex) { p.sendMessage("\u00a7cInvalid price."); }
                        });
                    })
            );
        }

        if (player.getUniqueId().equals(prop.getOwnerUUID())) {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "§cRemove Property",
                            "§7Permanently remove this property.",
                            "§cThis cannot be undone!"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getNationManager().deleteProperty(propertyId);
                        p.sendMessage("§aProperty removed.");
                        plugin.getGUIManager().openGUI(new CharacterPropertiesGUI(plugin, 0), p);
                    })
            );
        }

        if (prop.isForSale() && !prop.getOffers().isEmpty()) {
            int offerCount = prop.getOffers().size();
            addButton(16, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7eView Offers \u00a77(" + offerCount + ")",
                            "\u00a77Click to view purchase offers."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new PropertyOffersGUI(plugin, propertyId, type, contextId), (Player) e.getWhoClicked()))
            );
        }

        addButton(20, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a76Rooms",
                        "\u00a77Manage rooms within this property.",
                        "\u00a77Rooms: \u00a7f" + prop.getRoomIds().size()))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new RoomManagementGUI(plugin, propertyId, false, contextId, 0,
                            back -> plugin.getGUIManager().openGUI(new PropertyManageGUI(plugin, propertyId, type, contextId), back)), p);
                })
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (type == PropertyType.GOVERNMENT) {
                        plugin.getGUIManager().openGUI(new GovernmentPropertyDetailGUI(plugin, contextId, propertyId), p);
                    } else {
                        plugin.getGUIManager().openGUI(new CharacterPropertyDetailGUI(plugin, propertyId), p);
                    }
                })
        );
    }

    private void decorateBusiness(Player player) {
        BusinessPropertyData prop = plugin.getBusinessManager().loadProperty(propertyId);
        if (prop == null) return;

        com.dirt.data.BusinessData biz = plugin.getBusinessManager().loadBusiness(contextId);
        String bizName = biz != null ? biz.getName() : "Unknown";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR,
                        "\u00a7a" + prop.getName(),
                        "\u00a77Business: \u00a7f" + bizName,
                        "\u00a77Chunks: \u00a7f" + prop.getChunks().size()))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eRename Property",
                        "\u00a77Current: \u00a7f" + prop.getName()))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new property name:", input -> {
                        String name = input.trim();
                        if (name.isEmpty()) { p.sendMessage("\u00a7cName cannot be empty."); return; }
                        BusinessPropertyData pr = plugin.getBusinessManager().loadProperty(propertyId);
                        if (pr == null) return;
                        pr.setName(name);
                        plugin.getBusinessManager().saveProperty(pr);
                        p.sendMessage("\u00a7aProperty renamed to '\u00a76" + name + "\u00a7a'.");
                        plugin.getGUIManager().openGUI(new PropertyManageGUI(plugin, propertyId, type, contextId), p);
                    });
                })
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "\u00a7eExtend Property",
                        "\u00a77Add more chunks.",
                        "\u00a77Charged from business treasury."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChunkSelectionManager().startExtendBusinessProperty(p, contextId, propertyId);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a76Rooms",
                        "\u00a77Manage rooms within this property.",
                        "\u00a77Rooms: \u00a7f" + prop.getRoomIds().size()))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new RoomManagementGUI(plugin, propertyId, true, contextId, 0,
                            back -> plugin.getGUIManager().openGUI(new PropertyManageGUI(plugin, propertyId, type, contextId), back)), p);
                })
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new com.dirt.inventory.impl.business.BusinessPropertyDetailGUI(plugin, contextId, propertyId), (Player) e.getWhoClicked()))
        );
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