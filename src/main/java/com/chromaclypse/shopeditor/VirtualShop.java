package com.chromaclypse.shopeditor;

import java.util.Locale;
import java.util.UUID;

import com.chromaclypse.api.Log;
import com.chromaclypse.api.item.ItemBuilder;
import com.chromaclypse.api.messages.Text;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class VirtualShop {
	private Sign sign;
	private UUID owner;
	private int amount;
	private long price;
	private long refund;
	private ItemStack item;

	public static VirtualShop parse(Sign sign) {
		String[] lines = sign.getLines();

		Log.info("lines[0]: " + lines[0]);
		Log.info("lines[1]: " + lines[1]);
		Log.info("lines[2]: " + lines[2]);
		Log.info("lines[3]: " + lines[3]);

		UUID owner;
		{
			String[] first = lines[0].split(" ");
			String uuidString = first[first.length - 1];

			if(uuidString.isEmpty()) {
				Log.info("empty line");
				return null;
			}

			try {
				owner = UUID.fromString(uuidString);
			}
			catch(IllegalArgumentException e) {
				Log.info("offline uuid");
				owner = UUID.nameUUIDFromBytes(lines[0].getBytes());
			}
		}

		int amount;
		try {
			amount = Integer.parseInt(lines[1]);
		}
		catch(NumberFormatException e) {
			Log.info("bad amount");
			return null;
		}

		double price = 0.0;
		double refund = 0.0;
		{
			String[] bs = lines[2].split(":");

			if(bs.length > 2) {
				Log.info("Full price line");
				return null;
			}

			for(String part : bs) {
				Log.info("part: " + part);
				Double value = null;

				String key = null;

				for(String segment : part.trim().split(" ")) {
					if(segment.isEmpty()) {
						continue;
					}

					if(segment.toLowerCase(Locale.ENGLISH).equals("b")) {
						if(key != null) {
							Log.info("already found: " + key + " (b)");
							return null;
						}

						key = "b";
					}
					else if(segment.toLowerCase(Locale.ENGLISH).equals("s")) {
						if(key != null) {
							Log.info("already found: " + key + " (s)");
							return null;
						}

						key = "s";
					}
					else {
						try {
							value = Double.valueOf(segment);
						}
						catch(NumberFormatException e) {
							Log.info("bad number: " + segment);
							return null;
						}
					}
				}

				if(value == null) {
					Log.info("no number in " + part);
					return null;
				}
				else {
					if("b".equals(key)) {
						price = value.doubleValue();
					}
					else {
						refund = value.doubleValue();
					}
				}
			}
		}

		Material mat = Material.matchMaterial(lines[3].split("#")[0].replace(' ', '_'));

		Log.info("is good");
		VirtualShop shop = new VirtualShop();

		shop.sign = sign;
		shop.owner = owner;
		shop.amount = amount;
		shop.price = (long) Math.round(price * 100);
		shop.refund = (long) Math.round(refund) * 100;
		shop.item = mat != null ? new ItemStack(mat) : null;

		return shop;
	}

	private VirtualShop() {
	}

	public int getAmount() {
		return amount;
	}

	public String getPriceDisplay() {
		long priceMinor = Math.floorMod(price, 100);
		String priceString = Text.format().commas(price) + '.';
		String minor = String.valueOf(priceMinor);

		if(priceMinor < 10) {
			minor = "0" + minor;
		}

		return priceString + minor;
	}

	public String getRefundDisplay() {
		long refundMinor = Math.floorMod(refund, 100);
		String refundString = Text.format().commas(refund) + '.';
		String minor = String.valueOf(refundMinor);

		if(refundMinor < 10) {
			minor = "0" + minor;
		}

		return refundString + minor;
	}

	public long getPriceMajor() {
		return Math.floorDiv(price, 100);
	}

	public long getPriceMinor() {
		return Math.floorMod(price, 100);
	}

	public long getRefundMajor() {
		return Math.floorDiv(refund, 100);
	}

	public long getRefundMinor() {
		return Math.floorMod(refund, 100);
	}

	public ItemStack getItem() {
		return item;
	}

	public int setAmount(int amount) {
		return this.amount = Math.min(64, Math.max(1, amount));
	}

	public long setPriceMajor(long price) {
		setPrice(price * 100 + getPriceMinor());

		return getPriceMajor();
	}

	public long setPriceMinor(long priceMinor) {
		setPrice(price + priceMinor);

		return getPriceMinor();
	}

	public void setPrice(long fullPrice) {
		price = Math.max(fullPrice, 0);
		refund = Math.min(refund, price);
	}

	public long setRefundMajor(long refund) {
		setRefund(refund * 100 + getRefundMinor());

		return getRefundMajor();
	}

	public long setRefundMinor(long refundMinor) {
		setRefund(refund + refundMinor);

		return getRefundMinor();
	}

	public void setRefund(long fullRefund) {
		refund = Math.max(fullRefund, 0);
		price = Math.max(price, refund);
	}

	public void setItem(ItemStack item) {
		this.item = ItemBuilder.clone(item);
	}

	public void clear() {
		this.item = new ItemStack(Material.AIR);
	}

	public boolean editableBy(Player player) {
		return player.hasPermission("shopeditor.any") || player.getUniqueId().equals(owner);
	}

	public boolean updateAs(Player player) {
		ItemStack first = null;
		Chest chest = null;
		
		if(!editableBy(player)) {
			return false;
		}

		if(!(sign.getBlock().getState() instanceof Sign)) {
			return false;
		}

		if(item != null) {
			if(item.getType() != Material.AIR) {
				Block block = sign.getBlock().getRelative(BlockFace.DOWN);

				if(!(block.getState() instanceof Chest)) {
					return false;
				}

				chest = (Chest) block.getState();

				Inventory inv = chest.getInventory();
				first = inv.getItem(0);
				
				inv.setItem(0, item);

				sign.setLine(3, "?");
			}
			else {
				sign.setLine(0, "");
				sign.setLine(1, "");
				sign.setLine(2, "");
				sign.setLine(3, "");
			}
		}
		
		SignChangeEvent event = new SignChangeEvent(sign.getBlock(), player, sign.getLines());

		Bukkit.getPluginManager().callEvent(event);

		if(chest != null) {
			chest.getInventory().setItem(0, first);
		}

		if(!event.isCancelled()) {
			String lines[] = event.getLines();
			sign.setLine(0, lines[0]);
			sign.setLine(1, lines[1]);
			sign.setLine(2, lines[2]);
			sign.setLine(3, lines[3]);
			sign.update();

			player.closeInventory();
			return true;
		}
		else {
			// error message
			return false;
		}
	}
}
