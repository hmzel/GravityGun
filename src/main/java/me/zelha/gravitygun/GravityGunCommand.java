package me.zelha.gravitygun;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GravityGunCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;

        Player p = (Player) sender;
        ItemStack item = p.getItemInHand();

        if (item == null || item.getType() == Material.AIR) {
            p.sendMessage("§cMust be holding an item to use this command!");

            return true;
        }

        ItemMeta meta = item.getItemMeta();

        meta.addEnchant(Enchantment.ARROW_INFINITE, 131313, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName("§dGravity Gun");
        meta.setLore(Arrays.asList(
                "",
                "§dMode: §6Velocity",
                "",
                "§dLeft Click: Throw target",
                "§dRight Click: Pick up or drop target",
                "§dScroll Wheel: Move target further or closer",
                "§dDrop: Change mode"
        ));
        item.setItemMeta(meta);
        p.setItemInHand(item);

        return true;
    }
}
