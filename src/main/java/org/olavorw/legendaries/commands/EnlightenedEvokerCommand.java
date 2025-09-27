package org.olavorw.legendaries.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.olavorw.legendaries.boss.EnlightenedEvokerBoss;
import org.olavorw.legendaries.Legendaries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnlightenedEvokerCommand implements CommandExecutor, TabCompleter {
    private final Legendaries plugin;

    public EnlightenedEvokerCommand(Legendaries plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player target;
        if (args.length >= 1) {
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

        EnlightenedEvokerBoss boss = new EnlightenedEvokerBoss(plugin);
        boss.spawn(target.getLocation());
        sender.sendMessage(Component.text("Spawned The Enlightened Evoker at " + target.getName() + "!").color(NamedTextColor.GOLD));
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
