package org.olavorw.legendaries.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.olavorw.legendaries.IronDaggerManager;
import org.olavorw.legendaries.Legendaries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IronDaggerCommand implements CommandExecutor, TabCompleter {
    private final Legendaries plugin;
    private final IronDaggerManager manager;

    public IronDaggerCommand(Legendaries plugin, IronDaggerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 1) {
            sender.sendMessage(Component.text("Usage: /" + label + " [player]").color(NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[0]).color(NamedTextColor.RED));
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Component.text("Console must specify a player.").color(NamedTextColor.RED));
                return true;
            }
            target = p;
        }

        ItemStack dagger = manager.createIronDagger();
        target.getInventory().addItem(dagger);
        target.sendMessage(Component.text("You received a Deathripper Dagger!").color(NamedTextColor.GOLD));
        if (target != sender) {
            sender.sendMessage(Component.text("Gave a Deathripper Dagger to " + target.getName()).color(NamedTextColor.GOLD));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
            return out;
        }
        return List.of();
    }
}
