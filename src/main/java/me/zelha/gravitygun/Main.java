package me.zelha.gravitygun;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> runnableMap = new HashMap<>();
    private final Map<UUID, Double> distanceMap = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("gravitygun").setExecutor(new GravityGunCommand());
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        ItemStack item = p.getItemInHand();

        if (item == null || item.getType() == Material.AIR) return;
        if (!item.getItemMeta().getDisplayName().equals("§dGravity Gun")) return;
        if (!Arrays.toString(item.getItemMeta().getLore().toArray()).contains("§dMode: ")) return;

        e.setCancelled(true);

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (runnableMap.containsKey(uuid)) {
                Bukkit.getScheduler().cancelTask(runnableMap.get(uuid));
                runnableMap.remove(uuid);
                distanceMap.remove(uuid);

                return;
            }

            Location eye = p.getEyeLocation();
            Vector eyeVector = eye.toVector();
            Vector eyeDirection = eye.getDirection();
            Entity target = null;

            for (Entity entity : p.getWorld().getEntities()) {
                Vector toEntity = entity.getLocation().add(0, 1, 0).toVector().subtract(eyeVector);
                double dot = toEntity.normalize().dot(eyeDirection);

                if (dot > 0.99D) {
                    target = entity;

                    break;
                }
            }

            if (target == null) return;

            if (target instanceof LivingEntity) {
                distanceMap.put(uuid, p.getEyeLocation().distance(target.getLocation().add(0, ((LivingEntity) target).getEyeHeight(), 0)));
            } else {
                distanceMap.put(uuid, p.getEyeLocation().distance(target.getLocation()));
            }

            Entity entity = target;
            BukkitTask runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!entity.isValid()) {
                        runnableMap.remove(uuid);
                        distanceMap.remove(uuid);
                        cancel();

                        return;
                    }

                    Location wanted = p.getEyeLocation().add(p.getLocation().getDirection().multiply(distanceMap.get(uuid)));
                    Location current;

                    if (entity instanceof LivingEntity) {
                        current = entity.getLocation().add(0, ((LivingEntity) entity).getEyeHeight(), 0);
                    } else {
                        current = entity.getLocation();
                    }

                    entity.setVelocity(wanted.subtract(current).toVector());
                    entity.setFallDistance(0);
                }
            }.runTaskTimer(this, 0, 1);

            runnableMap.put(uuid, runnable.getTaskId());
        }
    }

    @EventHandler
    public void onScroll(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        ItemStack item = p.getItemInHand();

        if (!distanceMap.containsKey(uuid)) return;
        if (e.getNewSlot() == e.getPreviousSlot()) return;
        if (item == null || item.getType() == Material.AIR) return;
        if (!item.getItemMeta().getDisplayName().equals("§dGravity Gun")) return;
        if (!Arrays.toString(item.getItemMeta().getLore().toArray()).contains("§dMode: ")) return;

        e.setCancelled(true);

        int current = e.getPreviousSlot();
        int scroll = e.getNewSlot();

        if (current - scroll < 0 && scroll > current + 4) {
            distanceMap.put(uuid, distanceMap.get(uuid) + current + 8 - scroll + 1);
        } else if (scroll >= current - 4 && scroll < current) {
            distanceMap.put(uuid, distanceMap.get(uuid) + current - scroll);
        } else if (scroll - current < 0 && scroll < current - 4) {
            distanceMap.put(uuid, distanceMap.get(uuid) - ((8 - current) + scroll + 1));
        } else if (scroll > current) {
            distanceMap.put(uuid, distanceMap.get(uuid) - (scroll - current));
        }

        if (distanceMap.get(uuid) < 0) {
            distanceMap.put(uuid, 0D);
        }
    }
}



//Bukkit.broadcastMessage(Arrays.toString(item.getItemMeta().getLore().toArray()));










