package de.coolemod.donut.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordCommand implements CommandExecutor {
    private static final String DISCORD_URL = "https://discord.gg/gP8g4V2e";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§8[§9§lDiscord§8] §7Unser Discord: §b" + DISCORD_URL);

        if (sender instanceof Player player) {
            TextComponent link = new TextComponent("§b§nHier klicken, um dem Discord beizutreten");
            link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, DISCORD_URL));
            link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Discord öffnen")));
            player.spigot().sendMessage(link);
        }

        return true;
    }
}