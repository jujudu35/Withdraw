package net.betterwithdraw;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CheckListener implements Listener {

    private final BetterWithdraw plugin;

    public CheckListener(BetterWithdraw plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ne traiter que la main principale pour éviter les doubles clics
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        CheckManager checkManager = plugin.getCheckManager();

        // Vérification si c'est un chèque
        if (!checkManager.isCheck(item)) return;

        // On annule l'événement par sécurité (pour ne pas poser/utiliser le papier)
        event.setCancelled(true);

        double amount = checkManager.getCheckAmount(item);
        if (amount <= 0) return;

        // Retrait d'un chèque de la main du joueur
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Ajout de l'argent dans Vault
        plugin.getEconomy().depositPlayer(player, amount);

        // Message de confirmation
        String msg = plugin.getMessage("deposit-success")
                .replace("%amount%", String.format("%.2f", amount));
        player.sendMessage(msg);

        // Effet sonore
        playSound(player, "sounds.deposit-success");
    }

    private void playSound(Player player, String configPath) {
        if (!plugin.getConfig().getBoolean(configPath + ".enabled", true)) return;
        try {
            String soundName = plugin.getConfig().getString(configPath + ".sound", "ENTITY_PLAYER_LEVELUP");
            float volume = (float) plugin.getConfig().getDouble(configPath + ".volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble(configPath + ".pitch", 1.5);

            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {
            // Son invalide ou non supporté
        }
    }
}