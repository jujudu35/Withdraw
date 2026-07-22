package net.betterwithdraw;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CheckManager {

    private final BetterWithdraw plugin;
    private final NamespacedKey checkKey;
    private final NamespacedKey amountKey;

    public CheckManager(BetterWithdraw plugin) {
        this.plugin = plugin;
        this.checkKey = new NamespacedKey(plugin, "check");
        this.amountKey = new NamespacedKey(plugin, "amount");
    }

    /**
     * Crée un ItemStack de chèque de la valeur demandée.
     */
    public ItemStack createCheck(double amount, int count) {
        FileConfiguration config = plugin.getConfig();

        String materialName = config.getString("check-item.material", "PAPER");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.PAPER;

        ItemStack item = new ItemStack(material, count);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 1. Définition des clés PDC (PersistentDataContainer)
            meta.getPersistentDataContainer().set(checkKey, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(amountKey, PersistentDataType.DOUBLE, amount);

            // 2. Formatage du Nom et du Lore
            String formattedAmount = String.format("%.2f", amount);
            String name = config.getString("check-item.name", "&e&lChèque de &a%amount%$")
                    .replace("%amount%", formattedAmount);
            meta.setDisplayName(color(name));

            List<String> rawLore = config.getStringList("check-item.lore");
            List<String> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(color(line.replace("%amount%", formattedAmount)));
            }
            meta.setLore(lore);

            // 3. CustomModelData
            int cmd = config.getInt("check-item.custom-model-data", 0);
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }

            // 4. Glow (Effet d'enchantement)
            if (config.getBoolean("check-item.glow", true)) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack createCheck(double amount) {
        return createCheck(amount, 1);
    }

    /**
     * Vérifie si un ItemStack est un chèque valide de BetterWithdraw.
     */
    public boolean isCheck(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        Boolean isCheck = meta.getPersistentDataContainer().get(checkKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(isCheck);
    }

    /**
     * Récupère le montant d'un chèque.
     */
    public double getCheckAmount(ItemStack item) {
        if (!isCheck(item)) return 0.0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0.0;

        Double amount = meta.getPersistentDataContainer().get(amountKey, PersistentDataType.DOUBLE);
        return amount != null ? amount : 0.0;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}