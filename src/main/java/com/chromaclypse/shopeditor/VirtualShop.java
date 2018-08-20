package com.chromaclypse.shopeditor;

import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
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
    private double price;
    private double refund;
    private String itemString;

    public static VirtualShop parse(Sign sign) {
        String[] lines = sign.getLines();

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
            boolean hasPrice = false;
            boolean hasRefund = false;
            String[] bs = lines[2].split(":");

            if(bs.length > 2) {
                return null;
            }

            for(String part : bs) {
                boolean isBuy = true;
                boolean hasNumber = false;

                for(String segment : part.trim().split(" ")) {
                    if(segment.toLowerCase(Locale.ENGLISH).equals("b")) {
                        if(hasPrice || hasRefund) {
                            return null;
                        }

                        hasPrice = true;
                    }
                    else if(segment.toLowerCase(Locale.ENGLISH).equals("s")) {
                        if(hasPrice || hasRefund) {
                            return null;
                        }

                        isBuy = false;
                        hasRefund = true;
                    }
                    else {
                        double number;

                        try {
                            number = Double.parseDouble(segment);
                        }
                        catch(NumberFormatException e) {
                            return null;
                        }

                        if(isBuy) {
                            price = number;
                        }
                        else {
                            refund = number;
                        }

                        hasNumber = true;
                    }
                }

                if(!hasNumber) {
                    return null;
                }
            }
        }

        return new VirtualShop(sign, owner, amount, price, refund, lines[3]);
    }

    private VirtualShop(Sign sign, UUID owner, int amount, double price, double refund, String itemString) {
        this.sign = sign;
        this.owner = owner;
        this.amount = amount;
        this.price = price;
        this.refund = refund;
        this.itemString = itemString;
    }

    public int getAmount() {
        return amount;
    }

    public double getPrice() {
        return price;
    }

    public double getRefund() {
        return refund;
    }

    public int setAmount(int amount) {
        return this.amount = Math.min(64, Math.max(1, amount));
    }

    public double setPrice(double price) {
        if(price == 0.0) {
            return this.price = 0.0;
        }

        return this.price = Math.max(refund, price);
    }

    public double setRefund(double refund) {
        return this.price = Math.min(0, Math.max(price, refund));
    }

    public void setItem(String itemString) {
        this.itemString = itemString;
    }

    public void clear() {
        this.itemString = "";
    }

    public boolean updateAs(Player player) {
		Inventory inv = null;
        ItemStack first = null;
        
        if(!player.hasPermission("shopeditor.any") && !player.getUniqueId().equals(owner)) {
            return false;
        }

		if(itemString.equals("%hand%")) {
			ItemStack hand = player.getInventory().getItemInMainHand();

			Block block = sign.getBlock().getRelative(BlockFace.DOWN);

			if(!(block.getState() instanceof Chest)) {
				return false;
			}

			Chest chest = (Chest) block;

			inv = chest.getInventory();
			first = inv.getItem(0);
			inv.setItem(0, hand);
			sign.setLine(3, "?");
        }
        else if(itemString.isEmpty()) {
            sign.setLine(0, "");
            sign.setLine(1, "");
            sign.setLine(2, "");
            sign.setLine(3, "");
        }

		SignChangeEvent event = new SignChangeEvent(sign.getBlock(), player, sign.getLines());

		if(inv != null) {
			inv.setItem(0, first);
		}

		Bukkit.getPluginManager().callEvent(event);

		if(!event.isCancelled()) {
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
