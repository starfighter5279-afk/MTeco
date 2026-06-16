package com.mteco.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ShopData {
    public enum OwnerType { CHARACTER, BUSINESS }

    private UUID shopId;
    private OwnerType ownerType;
    private UUID ownerUUID;       // player UUID for CHARACTER, businessId for BUSINESS
    private String ownerDisplay;  // character name or business name
    private String chestLocation; // "world|x|y|z"
    private String signLocation;  // "world|x|y|z"
    private ItemStack itemTemplate; // 1-quantity template of the item being sold
    private double pricePerUnit;
    private List<Long> saleTimestamps = new ArrayList<>();
    private List<Double> saleAmounts = new ArrayList<>();
}