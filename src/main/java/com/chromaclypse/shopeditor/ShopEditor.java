package com.chromaclypse.shopeditor;

import java.util.HashSet;
import java.util.UUID;

import com.chromaclypse.api.Log;
import com.chromaclypse.api.item.ItemBuilder;
import com.chromaclypse.api.menu.Clicks;
import com.chromaclypse.api.menu.Menu;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ShopEditor implements Listener {
	private HashSet<UUID> pending = new HashSet<>();

	public void add(UUID uuid) {
		pending.add(uuid);
	}

	private void openShopEditor(VirtualShop shop, Player player) {
		Menu menu = new Menu(1, "Shop Editor");

		// Delete
		menu.put(0, new ItemBuilder(Material.RED_WOOL)
			.display("&cDelete this shop")
			.get(), click -> {
				shop.clear();
				shop.updateAs(player);
		});

		// Item
		menu.put(2, new ItemBuilder(Material.EMERALD)
			.display("Set shop item &7(in your hand)")
			.get(), click -> {
				ItemStack hand = ((Player) click.getWhoClicked()).getInventory().getItemInMainHand();

				if(hand != null && hand.getType() != Material.AIR) {
					shop.setItem("%hand%");
				}
		});

		// Quantity
		menu.put(3, new ItemStack(Material.RED_WOOL), click -> {
			shop.setAmount(Clicks.number(click, shop.getAmount(), 10));
		});

		// Price
		menu.put(5, new ItemStack(Material.RED_WOOL), click -> {
			double extra = shop.getPrice() - Math.floor(shop.getPrice());

			shop.setPrice(Clicks.number(click, (int)Math.floor(shop.getPrice()), 10) + extra);
		});

		// Refund
		menu.put(6, new ItemStack(Material.RED_WOOL), click -> {
			double extra = shop.getRefund() - Math.floor(shop.getRefund());

			shop.setRefund(Clicks.number(click, (int)Math.floor(shop.getRefund()), 10) + extra);
		});

		// Confirm
		menu.put(8, new ItemStack(Material.RED_WOOL), click -> {
			shop.updateAs(player);
		});

		player.openInventory(menu.getInventory());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onRightClick(PlayerInteractEvent event) {
		Log.info("click");

		if(event.getAction() == Action.RIGHT_CLICK_BLOCK &&
				event.getHand() == EquipmentSlot.HAND) {
					Log.info("right primary");

			BlockState block = event.getClickedBlock().getState();

			if(block instanceof Sign) {
				Log.info("sign");
				VirtualShop shop = VirtualShop.parse((Sign) block);

				if(shop != null && shop.editableBy(event.getPlayer())) {
					Log.info("shop");
					openShopEditor(shop, event.getPlayer());
					event.setCancelled(true);
				}
			}
		}
	}
}
