package com.chromaclypse.shopeditor;

import java.util.HashSet;

import com.chromaclypse.api.item.ItemBuilder;
import com.chromaclypse.api.menu.Menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ShopEditor<UUID> implements Listener {
	private HashSet<UUID> pending = new HashSet<>();

	public void add(UUID uuid) {
		pending.add(uuid);
	}

	private boolean canEdit(Player player, Sign sign) {
		return false;
	}

	private void fireSign(Player player, Sign sign) {
		Inventory inv = null;
		ItemStack first = null;

		if(sign.getLine(3).equals("%hand%")) {
			ItemStack hand = player.getInventory().getItemInMainHand();

			Block block = sign.getBlock().getRelative(BlockFace.DOWN);

			if(!(block.getState() instanceof Chest)) {
				return;
			}

			Chest chest = (Chest) block;

			inv = chest.getInventory();
			first = inv.getItem(0);
			inv.setItem(0, hand);
			sign.setLine(3, "?");
		}

		SignChangeEvent event = new SignChangeEvent(sign.getBlock(), player, sign.getLines());

		if(inv != null) {
			inv.setItem(0, first);
		}

		Bukkit.getPluginManager().callEvent(event);

		if(!event.isCancelled()) {
			sign.update();
			player.closeInventory();
		}
		else {
			// error message
		}
	}

	private Menu constructMenu(Sign sign) {
		Menu menu = new Menu(1, "Shop Editor");

		// Delete
		menu.put(0, new ItemBuilder(Material.RED_WOOL)
			.display("&cDelete this shop")
			.get(), click -> {
				sign.setLine(0, "");
				sign.setLine(1, "");
				sign.setLine(2, "");
				sign.setLine(3, "");
				fireSign((Player) click.getWhoClicked(), sign);
		});

		// Item
		menu.put(2, new ItemBuilder(Material.EMERALD)
			.display("Set shop item &7(in your hand)")
			.get(), click -> {
				ItemStack hand = ((Player) click.getWhoClicked()).getInventory().getItemInMainHand();

				if(hand != null && hand.getType() != Material.AIR) {
					sign.setLine(3, "%hand%");
				}
		});

		// Quantity
		menu.put(3, new ItemStack(Material.RED_WOOL), click -> {

		});

		// Price
		menu.put(5, new ItemStack(Material.RED_WOOL), click -> {

		});

		// Refund
		menu.put(6, new ItemStack(Material.RED_WOOL), click -> {

		});

		// Confirm
		menu.put(8, new ItemStack(Material.RED_WOOL), click -> {
			fireSign((Player) click.getWhoClicked(), sign);
		});

		return menu;
	}

	@EventHandler(ignoreCancelled = true)
	public void onRightClick(PlayerInteractEvent event) {

		if(event.getAction() == Action.RIGHT_CLICK_BLOCK &&
				event.getHand() == EquipmentSlot.HAND) {

			BlockState block = event.getClickedBlock().getState();

			if(block instanceof Sign &&
						canEdit(event.getPlayer(), (Sign) block) &&
						pending.remove(event.getPlayer().getUniqueId())) {
				Menu menu = constructMenu((Sign) block);

				event.getPlayer().openInventory(menu.getInventory());
			}
		}
	}
}
