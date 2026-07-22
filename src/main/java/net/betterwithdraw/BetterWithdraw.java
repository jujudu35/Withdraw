package net.betterwithdraw;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterWithdraw extends JavaPlugin {

    private static BetterWithdraw instance;
    private Economy economy;
    private CheckManager checkManager;

   @Override
    public void onEnable() {
        instance = this;

        // 1. Sauvegarde de la config par défaut
        saveDefaultConfig();

        // 2. Initialisation de Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault ou un plugin d'économie est introuvable ! Désactivation de BetterWithdraw.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Initialisation du CheckManager
        this.checkManager = new CheckManager(this);

        // 4. Enregistrement des événements et commandes
        getServer().getPluginManager().registerEvents(new CheckListener(this), this);

        WithdrawCommand commandExecutor = new WithdrawCommand(this);
        if (getCommand("withdraw") != null) {
            getCommand("withdraw").setExecutor(commandExecutor);
            getCommand("withdraw").setTabCompleter(commandExecutor);
        }
        if (getCommand("depositall") != null) {
            getCommand("depositall").setExecutor(commandExecutor);
            getCommand("depositall").setTabCompleter(commandExecutor);
        }

        getLogger().info("BetterWithdraw v1.0 activé avec succès ! (Support Vault OK)");
    }

        // 3. Initialisation du CheckManager
        this.checkManager = new CheckManager(this);

        getLogger().info("BetterWithdraw v1.0 activé avec succès ! (Support Vault OK)");
    }

    @Override
    public void onDisable() {
        getLogger().info("BetterWithdraw désactivé.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static BetterWithdraw getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public String getMessage(String path) {
        String prefix = getConfig().getString("messages.prefix", "&8[&eBetterWithdraw&8] ");
        String msg = getConfig().getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }
}