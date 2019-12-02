package com.chromaclypse.shopeditor;

import java.util.Locale;
import java.util.UUID;

import com.chromaclypse.api.item.ItemBuilder;
import com.chromaclypse.api.messages.Text;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
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

		if(lines[0].trim().isEmpty() && lines[1].trim().isEmpty() &&
				lines[2].trim().isEmpty() && lines[3].trim().isEmpty()) {
			VirtualShop emptyShop = new VirtualShop();
			emptyShop.sign = sign;
			emptyShop.owner = null;
			emptyShop.amount = 1;
			emptyShop.price = 0;
			emptyShop.refund = 0;
			emptyShop.item = null;

			return emptyShop;
		}

		UUID owner;
		{
			String[] first = lines[0].split(" ");
			String uuidString = first[first.length - 1];

			if(uuidString.isEmpty()) {
				return null;
			}

			try {
				owner = UUID.fromString(uuidString);
			}
			catch(IllegalArgumentException e) {
				owner = UUID.nameUUIDFromBytes(lines[0].getBytes());
			}
		}

		int amount;
		try {
			amount = Integer.parseInt(lines[1]);
		}
		catch(NumberFormatException e) {
			return null;
		}

		double price = 0.0;
		double refund = 0.0;
		{
			String[] bs = lines[2].split(":");

			if(bs.length > 2) {
				return null;
			}

			for(String part : bs) {
				Double value = null;

				String key = null;

				for(String segment : part.trim().split(" ")) {
					if(segment.isEmpty()) {
						continue;
					}

					if(segment.toLowerCase(Locale.ENGLISH).contains("b")) {
						if(key != null) {
							return null;
						}

						key = "b";
					}
					else if(segment.toLowerCase(Locale.ENGLISH).contains("s")) {
						if(key != null) {
							return null;
						}

						key = "s";
					}

					try {
						value = Double.valueOf(segment.replaceAll("[BbSs]", ""));
					}
					catch(NumberFormatException e) {
					}
				}

				if(value == null) {
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

		VirtualShop shop = new VirtualShop();

		shop.sign = sign;
		shop.owner = owner;
		shop.amount = amount;
		shop.price = (long) Math.round(price * 100);
		shop.refund = (long) Math.round(refund * 100);
		shop.item = mat != null ? new ItemStack(mat) : null;

		return shop;
	}

	private VirtualShop() {
	}

	public int getAmount() {
		return amount;
	}

	public String getPriceDisplay() {
		long priceMinor = price % 100;
		String priceString = Text.format().commas(price / 100) + '.';
		String minor = String.valueOf(priceMinor);

		if(priceMinor < 10) {
			minor = "0" + minor;
		}

		return priceString + minor;
	}

	public String getRefundDisplay() {
		long refundMinor = refund % 100;
		String refundString = Text.format().commas(refund / 100) + '.';
		String minor = String.valueOf(refundMinor);

		if(refundMinor < 10) {
			minor = "0" + minor;
		}

		return refundString + minor;
	}

	public long getPriceMajor() {
		return price / 100;
	}

	public long getPriceMinor() {
		return price % 100;
	}

	public long getRefundMajor() {
		return refund / 100;
	}

	public long getRefundMinor() {
		return refund % 100;
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
		setPrice(getPriceMajor() * 100 + priceMinor);

		return getPriceMinor();
	}

	public void setPrice(long fullPrice) {
		price = Math.max(fullPrice, 0);

		if(price > 0) {
			refund = Math.min(refund, price);
		}
	}

	public long setRefundMajor(long refund) {
		setRefund(refund * 100 + getRefundMinor());

		return getRefundMajor();
	}

	public long setRefundMinor(long refundMinor) {
		setRefund(getRefundMajor() * 100 + refundMinor);

		return getRefundMinor();
	}

	public void setRefund(long fullRefund) {
		refund = Math.max(fullRefund, 0);

		if(price > 0) {
			price = Math.max(price, refund);
		}
	}

	public void setItem(ItemStack item) {
		this.item = ItemBuilder.clone(item);
		this.item.setAmount(1);
	}

	public void clear() {
		this.item = new ItemStack(Material.AIR);
	}

	public boolean editableBy(Player player) {
		return owner == null || player.hasPermission("shopeditor.any") || player.getUniqueId().equals(owner);
	}

	private static String decimate(long number) {
		String display = String.valueOf(number);

		switch(display.length()) {
			case 1:
				display =  "0.0" + display;
				break;

			case 2:
				display =  "0." + display;
				break;

			default: {
				int decimalPos = display.length() - 2;

				display = display.substring(0, decimalPos) + '.' + display.substring(decimalPos);
				break;
			}
		}

		return display.replace(".00", "");
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

		if(sign.getLine(0).trim().isEmpty()) {
			if(owner == null) {
				owner = player.getUniqueId();
			}

			sign.setLine(0, Bukkit.getOfflinePlayer(owner).getName() + ' ' + owner.toString().toLowerCase());
		}

		sign.setLine(1, String.valueOf(amount));

		String priceline = "";

		if(price > 0) {
			priceline += "B " + decimate(price);
		}
		
		if(refund > 0) {
			if(!priceline.isEmpty()) {
				priceline += " : ";
			}

			priceline += "S " + decimate(refund);
		}
		sign.setLine(2, priceline);

		if(item != null) {
			if(item.getType() != Material.AIR) {
				Block block = sign.getBlock().getRelative(BlockFace.DOWN);

				if(!(block.getState() instanceof Chest)) {
					BlockData data = sign.getBlockData();
					
					if(data instanceof Directional) {
						block = sign.getBlock().getRelative(((Directional)data).getFacing().getOppositeFace());
					}
					
					if(!(block.getState() instanceof Chest)) {
						return false;
					}
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
	
	public UUID getOwner() {
		return owner;
	}
	
	public Block getBlock() {
		return sign.getBlock();
	}

	public boolean isValid() {
		return item != null && (price > 0 || refund > 0);
	}
}
