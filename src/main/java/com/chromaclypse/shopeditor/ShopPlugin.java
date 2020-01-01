package com.chromaclypse.shopeditor;

import com.chromaclypse.api.command.CommandBase;
import com.chromaclypse.api.command.Context;
import com.chromaclypse.api.messages.Text;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopPlugin extends JavaPlugin {
	private ShopEditor editor = new ShopEditor();

	@Override
	public void onEnable() {
		getCommand("shopeditor").setExecutor(new CommandBase().calls(this::command).getCommand());
		getServer().getPluginManager().registerEvents(editor, this);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
	}
	
	public boolean command(Context context) {
		Player player = context.Player();

		editor.add(player.getUniqueId());
		player.sendMessage(Text.format().colorize("&aRight click your shop (sign) to edit"));
		return true;
	}
}
