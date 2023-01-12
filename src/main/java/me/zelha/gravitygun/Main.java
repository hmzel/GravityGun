package me.zelha.gravitygun;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin implements Listener {

    private static Main instance;
    private final Map<UUID, PlayerData> dataMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        getCommand("gravitygun").setExecutor(new GravityGunCommand());
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        ItemStack item = p.getItemInHand();

        if (item == null || item.getType() == Material.AIR) return;

        List<String> lore = item.getItemMeta().getLore();

        if (!item.getItemMeta().getDisplayName().equals("§dGravity Gun")) return;
        if (lore.size() < 2 || !lore.get(1).contains("§dMode: ")) return;

        e.setCancelled(true);

        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && dataMap.containsKey(uuid)) {
            dataMap.get(uuid).stop(true);
        }

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (dataMap.containsKey(uuid)) {
                dataMap.get(uuid).stop(false);

                return;
            }

            for (Mode mode : Mode.values()) {
                if (lore.get(1).contains(mode.getCheck())) {
                    dataMap.put(uuid, new PlayerData(p, mode, lore));

                    break;
                }
            }
        }
    }

    @EventHandler
    public void onScroll(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        ItemStack item = p.getItemInHand();

        if (e.getNewSlot() == e.getPreviousSlot()) return;
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();

        if (!meta.getDisplayName().equals("§dGravity Gun")) return;
        if (lore.size() < 2 || !lore.get(1).contains("§dMode: ")) return;
        if (!dataMap.containsKey(uuid) && !(p.isSneaking() && lore.get(1).contains(Mode.BLOCKS.getCheck()))) return;

        e.setCancelled(true);

        int current = e.getPreviousSlot();
        int scroll = e.getNewSlot();
        int amount = 0;

        if (current - scroll < 0 && scroll > current + 4) {
            amount = current + 8 - scroll + 1;
        } else if (scroll >= current - 4 && scroll < current) {
            amount = current - scroll;
        } else if (scroll - current < 0 && scroll < current - 4) {
            amount = -((8 - current) + scroll + 1);
        } else if (scroll > current) {
            amount = -(scroll - current);
        }

        if (p.isSneaking() && lore.get(1).contains(Mode.BLOCKS.getCheck())) {
            int area = Integer.parseInt(lore.get(2).replace("§dArea: §b", ""));

            if (area + amount < 1) return;

            lore.set(2, "§dArea: §b" + (area + amount));
            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            dataMap.get(uuid).setDistance(dataMap.get(uuid).getDistance() + amount);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        ItemStack item = e.getItemDrop().getItemStack();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();

        if (!meta.getDisplayName().equals("§dGravity Gun")) return;
        if (lore.size() < 2 || !lore.get(1).contains("§dMode: ")) return;

        e.setCancelled(true);

        if (dataMap.containsKey(uuid)) {
            dataMap.get(uuid).stop(false);
        }

        if (lore.get(1).contains(Mode.VELOCITY.getCheck())) {
            lore.set(1, "§dMode: §5Teleport");
        } else if (lore.get(1).contains(Mode.TELEPORT.getCheck())) {
            lore.set(1, "§dMode: §9Blocks");
            lore.add(2, "§dArea: §b1");
            lore.add("§dShift + Scroll Wheel: Increase area to pick up");
        } else if (lore.get(1).contains(Mode.BLOCKS.getCheck())) {
            lore.set(1, "§dMode: §6Velocity");
            lore.remove(2);
            lore.remove(7);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public void removeData(UUID uuid) {
        dataMap.remove(uuid);
    }

    public static Main getInstance() {
        return instance;
    }
}










