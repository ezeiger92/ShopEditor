package com.chromaclypse.shopeditor;

import com.chromaclypse.api.messages.Text;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopPlugin extends JavaPlugin {
	private ShopEditor editor = new ShopEditor();

	@Override
	public void onEnable() {
		getCommand("shopeditor").setExecutor(this);
		getServer().getPluginManager().registerEvents(editor, this);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;

			editor.add(player.getUniqueId());
			sender.sendMessage(Text.format().colorize("&aRight click your shop (sign) to edit"));
		}
		else {
			sender.sendMessage(Text.format().colorize("&cMust be a player"));
		}

		return true;
	}
}
