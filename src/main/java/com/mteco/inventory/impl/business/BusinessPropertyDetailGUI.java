package com.mteco.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessPropertyData;
import com.mteco.data.BusinessRole;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.inventory.impl.nation.PropertyPermissionsGUI;
import com.mteco.inventory.impl.nation.RolePermsGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BusinessPropertyDetailGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;
    private final UUID propertyId;

    public BusinessPropertyDetailGUI(MTeco plugin, UUID businessId, UUID propertyId) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.propertyId = propertyId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7aBusiness Property Detail");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessPropertyData prop = plugin.getBusinessManager().loadProperty(propertyId);
        if (prop == null) { super.decorate(player); return; }

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        String bizName = biz != null ? biz.getName() : "Unknown";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR,
                        "\u00a7a" + prop.getName(),
                        "\u00a77Business: \u00a7f" + bizName,
                        "\u00a77Chunks: \u00a7f" + prop.getChunks().size(),
                        "\u00a7eClick to manage"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new com.mteco.inventory.impl.nation.PropertyManageGUI(plugin, propertyId,
                            com.mteco.inventory.impl.nation.PropertyManageGUI.PropertyType.BUSINESS, businessId), p);
                })
        );

        boolean isOwner = biz != null && player.getUniqueId().equals(biz.getOwnerUUID());
        if (isOwner) {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "\u00a7cDelete Property",
                            "\u00a77Permanently remove this business property.",
                            "\u00a7cThis cannot be undone!"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getBusinessManager().deleteProperty(propertyId);
                        p.sendMessage("\u00a7aBusiness property deleted.");
                        plugin.getGUIManager().openGUI(new BusinessPropertyGUI(plugin, businessId, 0), p);
                    })
            );
        }

        if (plugin.getSettings().isPropertyPermissionsEnabled()) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "\u00a7ePermissions",
                            "\u00a77Manage who can interact",
                            "\u00a77with this property."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        BusinessPropertyData pr = plugin.getBusinessManager().loadProperty(propertyId);
                        if (pr == null) return;
                        PropertyPermissionsGUI permsGui = new PropertyPermissionsGUI(plugin,
                                pr.getPermsSameRegion(), pr.getPermsSameNation(), pr.getPermsForeign(),
                                () -> plugin.getBusinessManager().saveProperty(pr),
                                back -> plugin.getGUIManager().openGUI(new BusinessPropertyDetailGUI(plugin, businessId, propertyId), back),
                                pr.getDeniedMobs(),
                                () -> plugin.getBusinessManager().saveProperty(pr),
                                back -> plugin.getGUIManager().openGUI(new BusinessPropertyDetailGUI(plugin, businessId, propertyId), back));
                        BusinessData innerBiz = plugin.getBusinessManager().loadBusiness(businessId);
                        if (innerBiz != null && !innerBiz.getRoles().isEmpty()) {
                            permsGui.setRolePermsOpener(rp -> {
                                BusinessPropertyData rPr = plugin.getBusinessManager().loadProperty(propertyId);
                                if (rPr == null) return;
                                BusinessData rBiz = plugin.getBusinessManager().loadBusiness(businessId);
                                if (rBiz == null) return;
                                List<UUID> rIds = new ArrayList<>();
                                List<String> rNames = new ArrayList<>();
                                List<String> rColors = new ArrayList<>();
                                RolePermsGUI.buildBizRoles(rBiz, rIds, rNames, rColors);
                                plugin.getGUIManager().openGUI(new RolePermsGUI(plugin, rPr.getRolePerms(),
                                        rIds, rNames, rColors,
                                        () -> plugin.getBusinessManager().saveProperty(rPr),
                                        back -> plugin.getGUIManager().openGUI(new BusinessPropertyDetailGUI(plugin, businessId, propertyId), back),
                                        0), rp);
                            });
                        }
                        plugin.getGUIManager().openGUI(permsGui, p);
                    })
            );
        }

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessPropertyGUI(plugin, businessId, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}