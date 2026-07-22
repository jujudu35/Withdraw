package net.betterwithdraw;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WithdrawCommand implements CommandExecutor, TabCompleter {

    private final BetterWithdraw plugin;

    public WithdrawCommand(BetterWithdraw plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Commende /depositall
        if (command.getName().equalsIgnoreCase("depositall")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getMessage("must-be-player"));
                return true;
            }

            if (!player.hasPermission("betterwithdraw.depositall")) {
                player.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }

            double totalDeposited = 0.0;
            int count = 0;
            CheckManager checkManager = plugin.getCheckManager();

            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (checkManager.isCheck(item)) {
                    double amount = checkManager.getCheckAmount(item);
                    int stackAmount = item.getAmount();

                    totalDeposited += (amount * stackAmount);
                    count += stackAmount;

                    player.getInventory().setItem(i, null);
                }
            }

            if (count > 0) {
                plugin.getEconomy().depositPlayer(player, totalDeposited);
                String msg = plugin.getMessage("depositall-success")
                        .replace("%count%", String.valueOf(count))
                        .replace("%amount%", String.format("%.2f", totalDeposited));
                player.sendMessage(msg);
                playSound(player, "sounds.deposit-success");
            } else {
                player.sendMessage(plugin.getMessage("no-checks-found"));
            }
            return true;
        }

        // Commende /withdraw
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("invalid-amount")
                    .replace("%min%", String.valueOf(plugin.getConfig().getDouble("settings.min-withdraw", 10.0))));
            return true;
        }

        // Subcommand: reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("betterwithdraw.reload")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(plugin.getMessage("reload-success"));
            return true;
        }

        // Subcommand: give <joueur> <montant>
        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("betterwithdraw.give")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /withdraw give <joueur> <montant>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getMessage("player-not-found"));
                return true;
            }
            try {
                double amount = Double.parseDouble(args[2]);
                if (amount <= 0) throw new NumberFormatException();

                ItemStack check = plugin.getCheckManager().createCheck(amount);
                target.getInventory().addItem(check);

                sender.sendMessage(plugin.getMessage("give-success-sender")
                        .replace("%amount%", String.format("%.2f", amount))
                        .replace("%player%", target.getName()));
                target.sendMessage(plugin.getMessage("give-success-receiver")
                        .replace("%amount%", String.format("%.2f", amount)));
                playSound(target, "sounds.withdraw-success");
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessage("invalid-amount")
                        .replace("%min%", "0"));
            }
            return true;
        }

        // Création de chèque directe par un joueur (/withdraw <montant|all>)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("must-be-player"));
            return true;
        }

        if (!player.hasPermission("betterwithdraw.use")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        double balance = plugin.getEconomy().getBalance(player);
        double amountToWithdraw;

        if (args[0].equalsIgnoreCase("all")) {
            amountToWithdraw = balance;
        } else {
            try {
                amountToWithdraw = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("invalid-amount")
                        .replace("%min%", String.valueOf(plugin.getConfig().getDouble("settings.min-withdraw", 10.0))));
                return true;
            }
        }

        double min = plugin.getConfig().getDouble("settings.min-withdraw", 10.0);
        double max = plugin.getConfig().getDouble("settings.max-withdraw", 1000000.0);

        if (amountToWithdraw < min || amountToWithdraw > max) {
            player.sendMessage(plugin.getMessage("invalid-amount").replace("%min%", String.valueOf(min)));
            playSound(player, "sounds.error");
            return true;
        }

        if (balance < amountToWithdraw) {
            player.sendMessage(plugin.getMessage("not-enough-money"));
            playSound(player, "sounds.error");
            return true;
        }

        // Prélèvement et création
        plugin.getEconomy().withdrawPlayer(player, amountToWithdraw);
        ItemStack check = plugin.getCheckManager().createCheck(amountToWithdraw);
        player.getInventory().addItem(check);

        player.sendMessage(plugin.getMessage("withdraw-success")
                .replace("%amount%", String.format("%.2f", amountToWithdraw)));
        playSound(player, "sounds.withdraw-success");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("withdraw")) {
            if (args.length == 1) {
                completions.add("all");
                completions.add("100");
                completions.add("500");
                if (sender.hasPermission("betterwithdraw.reload")) completions.add("reload");
                if (sender.hasPermission("betterwithdraw.give")) completions.add("give");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }

    private void playSound(Player player, String configPath) {
        if (!plugin.getConfig().getBoolean(configPath + ".enabled", true)) return;
        try {
            String soundName = plugin.getConfig().getString(configPath + ".sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            float volume = (float) plugin.getConfig().getDouble(configPath + ".volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble(configPath + ".pitch", 1.2);

            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {
        }
    }
}