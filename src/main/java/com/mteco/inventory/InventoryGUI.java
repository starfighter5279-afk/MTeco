package com.mteco.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.util.ItemUtil;

import java.util.HashMap;
import java.util.Map;

public abstract class InventoryGUI implements InventoryHandler {
    private Inventory inventory;
    private final Map<Integer, InventoryButton> buttonMap = new HashMap<>();

    public InventoryGUI() {}

    public Inventory getInventory() {
        if (this.inventory == null) {
            this.inventory = this.createInventory();
        }
        return this.inventory;
    }

    public void addButton(int slot, InventoryButton button) {
        this.buttonMap.put(slot, button);
    }

    public Map<Integer, InventoryButton> getButtonMap() {
        return this.buttonMap;
    }

    public void decorate(Player player) {
        this.buttonMap.forEach((slot, button) -> {
            try {
                ItemStack icon = button.getIconCreator().apply(player);
                this.getInventory().setItem(slot, icon);
            } catch (Exception e) {
                this.getInventory().setItem(slot, new ItemStack(org.bukkit.Material.BARRIER));
            }
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        InventoryButton button = this.buttonMap.get(slot);
        if (button != null && button.getEventConsumer() != null) {
            button.getEventConsumer().accept(event);
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {}

    @Override
    public void onClose(InventoryCloseEvent event) {}

    protected void fillGlass(int size) {
        for (int i = 0; i < size; i++) {
            final boolean border = isBorderSlot(i, size);
            addButton(i, new InventoryButton()
                    .creator(p -> border ? ItemUtil.buildGlassPane(XMaterial.GRAY_STAINED_GLASS_PANE) : ItemUtil.buildGlassPane())
                    .consumer(e -> {}));
        }
    }

    protected void fillGlass(int size, XMaterial accent) {
        for (int i = 0; i < size; i++) {
            final boolean border = isBorderSlot(i, size);
            addButton(i, new InventoryButton()
                    .creator(p -> border ? ItemUtil.buildGlassPane(accent) : ItemUtil.buildGlassPane())
                    .consumer(e -> {}));
        }
    }

    protected void fillPagedGui(int size, XMaterial accent) {
        for (int i = 0; i < size; i++) {
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane()).consumer(e -> {}));
        }
        int navStart = size - 9;
        for (int i = navStart; i < size; i++) {
            final int slot = i;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildGlassPane(accent))
                    .consumer(e -> {}));
        }
    }

    protected void addPageIndicator(int slot, int currentPage, int totalPages) {
        addButton(slot, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7e\u00a7lPage " + (currentPage + 1) + " \u00a77/ \u00a7e" + totalPages))
                .consumer(e -> {}));
    }

    private static boolean isBorderSlot(int slot, int size) {
        int row = slot / 9;
        int col = slot % 9;
        int maxRow = (size / 9) - 1;
        return row == 0 || row == maxRow || col == 0 || col == 8;
    }

    protected abstract Inventory createInventory();

    public String getBedrockTitle() {
        return "Menu";
    }
}