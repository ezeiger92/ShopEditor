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

	private static ItemStack makeItemButton(VirtualShop shop) {
		return new ItemBuilder(shop.getItem())
			.display("Set shop item &7(in your hand)")
			.get();
	}

	private static ItemStack makeQuantityButton(VirtualShop shop) {
		return new ItemBuilder(Material.EMERALD)
			.amount(shop.getAmount())
			.display("Set transaction amount")
			.get();
	}

	private static ItemStack makePriceMajorButton(VirtualShop shop) {
		return new ItemBuilder(Material.GOLD_INGOT)
			.wrapText("&aPrice: " + shop.getPriceDisplay(),
			"Click to set major price")
			.get();
	}

	private static ItemStack makePriceMinorButton(VirtualShop shop) {
		return new ItemBuilder(Material.GOLD_NUGGET)
			.wrapText("&aPrice: " + shop.getPriceDisplay(),
			"Click to set minor price")
			.get();
	}

	private static ItemStack makeRefundMajorButton(VirtualShop shop) {
		return new ItemBuilder(Material.IRON_INGOT)
			.wrapText("&aRefund: " + shop.getRefundDisplay(),
			"Click toset major price")
			.get();
	}

	private static ItemStack makeRefundMinorButton(VirtualShop shop) {
		return new ItemBuilder(Material.IRON_NUGGET)
			.wrapText("&aRefund: " + shop.getRefundDisplay(),
			"Click to set minor price")
			.get();
	}

	private void openShopEditor(VirtualShop shop, Player player) {
		Menu menu = new Menu(2, "Shop Editor");

		// Delete
		menu.put(0, new ItemBuilder(Material.RED_WOOL)
			.display("&cDelete shop")
			.get(), click -> {
				shop.clear();
				shop.updateAs(player);
		});

		// Item
		menu.put(2, makeItemButton(shop), click -> {
				ItemStack hand = ((Player) click.getWhoClicked()).getInventory().getItemInMainHand();

				if(hand != null && hand.getType() != Material.AIR) {
					shop.setItem(hand);
					menu.getInventory().setItem(2, makeItemButton(shop));
				}
		});

		// Quantity
		menu.put(3, makeQuantityButton(shop), click -> {
				shop.setAmount(Clicks.number(click, shop.getAmount(), 10));
				menu.getInventory().setItem(3, makeQuantityButton(shop));
		});

		// Price
		menu.put(5, makePriceMajorButton(shop), click -> {
			shop.setPriceMajor(Clicks.number(click, shop.getPriceMajor(), 10));
			menu.getInventory().setItem(5, makePriceMajorButton(shop));
		});

		// Price Minor
		menu.put(14, makePriceMinorButton(shop), click -> {
			shop.setPriceMinor(Clicks.number(click, shop.getPriceMinor(), 10));
			menu.getInventory().setItem(14, makePriceMinorButton(shop));
		});

		// Refund
		menu.put(6, makeRefundMajorButton(shop), click -> {
			shop.setRefundMajor(Clicks.number(click, shop.getRefundMajor(), 10));
			menu.getInventory().setItem(6, makeRefundMajorButton(shop));
		});

		// Refund Minor
		menu.put(15, makeRefundMinorButton(shop), click -> {
			shop.setRefundMinor(Clicks.number(click, shop.getRefundMinor(), 10));
			menu.getInventory().setItem(15, makeRefundMinorButton(shop));
		});

		// Confirm
		menu.put(8, new ItemBuilder(Material.GREEN_WOOL)
			.display("&2Update shop")
			.get(), click -> {
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

				if(shop != null && shop.editableBy(event.getPlayer()) &&
						pending.remove(event.getPlayer().getUniqueId())) {
					Log.info("shop");
					openShopEditor(shop, event.getPlayer());
					event.setCancelled(true);
				}
			}
		}
	}
}
