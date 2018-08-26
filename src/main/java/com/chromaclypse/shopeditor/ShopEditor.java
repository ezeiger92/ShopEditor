package com.chromaclypse.shopeditor;

import java.util.HashSet;
import java.util.UUID;

import com.chromaclypse.api.item.ItemBuilder;
import com.chromaclypse.api.menu.Clicks;
import com.chromaclypse.api.menu.Menu;
import com.chromaclypse.api.messages.Text;

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

	private static void updateShopButtons(VirtualShop shop, Menu menu) {
		menu.getInventory().setItem( 5, makePriceMajorButton(shop));
		menu.getInventory().setItem(14, makePriceMinorButton(shop));
		menu.getInventory().setItem( 6, makeRefundMajorButton(shop));
		menu.getInventory().setItem(15, makeRefundMinorButton(shop));
		menu.getInventory().setItem(8, makeConfirmButton(shop));
	}

	private static String nameOf(ItemStack stack) {
		if(stack == null) {
			return "-None-";
		}

		if(stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
			return stack.getItemMeta().getDisplayName();
		}

		return Text.format().niceName(stack.getType().name());
	}

	private static ItemStack makeItemButton(VirtualShop shop) {
		ItemBuilder builder;

		if(shop.getItem() == null) {
			builder = new ItemBuilder(Material.BARRIER);
		}
		else {
			builder = new ItemBuilder(shop.getItem());
		}

		return builder
			.wrapText("&3Item: &f" + nameOf(shop.getItem()),
				"Set shop item &7(in your hand)")
			.get();
	}

	private static ItemStack makeQuantityButton(VirtualShop shop) {
		return new ItemBuilder(Material.EMERALD)
			.amount(shop.getAmount())
			.wrapText("&3Amount: &f" + shop.getAmount(),
			"&7&oSet transaction amount",
			" &7Left: &f+1 &7(Shift x8)",
			" &7Right: &f-1 &7(Shift x8)")
			.get();
	}

	private static ItemStack makePriceMajorButton(VirtualShop shop) {
		return new ItemBuilder(Material.GOLD_INGOT)
			.wrapText("&aPrice: &f" + shop.getPriceDisplay().replace(".", "&7."),
			"&7&oClick to set major price",
			" &7Left: &f+1 &7(Shift x10)",
			" &7Right: &f-1 &7(Shift x10)")
			.get();
	}

	private static ItemStack makePriceMinorButton(VirtualShop shop) {
		return new ItemBuilder(Material.GOLD_NUGGET)
			.wrapText("&aPrice: &7" + shop.getPriceDisplay().replace(".", ".&f"),
			"&7&oClick to set minor price",
			" &7Left: &f+1 &7(Shift x10)",
			" &7Right: &f-1 &7(Shift x10)")
			.get();
	}

	private static ItemStack makeRefundMajorButton(VirtualShop shop) {
		return new ItemBuilder(Material.IRON_INGOT)
			.wrapText("&eRefund: &f" + shop.getRefundDisplay().replace(".", "&7."),
			"&7&oClick to set major refund",
			" &7Left: &f+1 &7(Shift x10)",
			" &7Right: &f-1 &7(Shift x10)")
			.get();
	}

	private static ItemStack makeRefundMinorButton(VirtualShop shop) {
		return new ItemBuilder(Material.IRON_NUGGET)
			.wrapText("&eRefund: &7" + shop.getRefundDisplay().replace(".", ".&f"),
			"&7&oClick to set minor refund",
			" &7Left: &f+1 &7(Shift x10)",
			" &7Right: &f-1 &7(Shift x10)")
			.get();
	}

	private static ItemStack makeConfirmButton(VirtualShop shop) {
		String prefix;

		if(shop.isValid()) {
			prefix = "&2";
		}
		else {
			prefix = "&8";
		}

		return new ItemBuilder(Material.GREEN_CONCRETE)
			.wrapText(prefix + "Update shop",
			" &3Item: &f" + nameOf(shop.getItem()),
			" &3Amount: &f" + shop.getAmount(),
			" &aPrice: &f" + shop.getPriceDisplay(),
			" &eRefund: &f" + shop.getRefundDisplay())
			.get();
	}

	private void openShopEditor(VirtualShop shop, Player player) {
		Menu menu = new Menu(2, "Shop Editor");

		// Delete
		menu.put(0, new ItemBuilder(Material.BARRIER)
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
					menu.getInventory().setItem(8, makeConfirmButton(shop));
				}
		});

		// Quantity
		menu.put(3, makeQuantityButton(shop), click -> {
				shop.setAmount(Clicks.number(click, shop.getAmount(), 8));
				menu.getInventory().setItem(3, makeQuantityButton(shop));
				menu.getInventory().setItem(8, makeConfirmButton(shop));
		});

		// Price
		menu.put(5, makePriceMajorButton(shop), click -> {
			shop.setPriceMajor(Clicks.number(click, shop.getPriceMajor(), 10));
			updateShopButtons(shop, menu);
		});

		// Price Minor
		menu.put(14, makePriceMinorButton(shop), click -> {
			shop.setPriceMinor(Clicks.number(click, shop.getPriceMinor(), 10));
			updateShopButtons(shop, menu);
		});

		// Refund
		menu.put(6, makeRefundMajorButton(shop), click -> {
			shop.setRefundMajor(Clicks.number(click, shop.getRefundMajor(), 10));
			updateShopButtons(shop, menu);
		});

		// Refund Minor
		menu.put(15, makeRefundMinorButton(shop), click -> {
			shop.setRefundMinor(Clicks.number(click, shop.getRefundMinor(), 10));
			updateShopButtons(shop, menu);
		});

		// Confirm
		menu.put(8, makeConfirmButton(shop), click -> {
			if(shop.isValid()) {
				shop.updateAs(player);
			}
		});

		player.openInventory(menu.getInventory());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onRightClick(PlayerInteractEvent event) {
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK &&
				event.getHand() == EquipmentSlot.HAND) {
			BlockState block = event.getClickedBlock().getState();

			if(block instanceof Sign) {
				VirtualShop shop = VirtualShop.parse((Sign) block);

				Player player = event.getPlayer();

				if(shop != null && shop.editableBy(player)) {
					if(pending.remove(player.getUniqueId()) ||
							player.getInventory().getItemInMainHand().getType() == Material.INK_SAC) {
						openShopEditor(shop, player);
						event.setCancelled(true);
					}
				}
			}
		}
	}
}
