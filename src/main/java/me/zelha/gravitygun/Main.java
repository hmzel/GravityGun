package me.zelha.gravitygun;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public final class Main extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> runnableMap = new HashMap<>();
    private final Map<UUID, Double> distanceMap = new HashMap<>();
    private final Map<UUID, UUID> targetMap = new HashMap<>();

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

        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && targetMap.containsKey(uuid)) {
            Entity entity = getEntity(targetMap.get(uuid));
            Location entityLoc = entity.getLocation();

            if (entity instanceof LivingEntity) {
                entityLoc = ((LivingEntity) entity).getEyeLocation();
            }

            Bukkit.getScheduler().cancelTask(runnableMap.get(uuid));
            runnableMap.remove(uuid);
            distanceMap.remove(uuid);
            targetMap.remove(uuid);

            entity.setVelocity(entityLoc.subtract(p.getEyeLocation()).toVector().normalize().multiply(2.5));
        }

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (runnableMap.containsKey(uuid)) {
                Bukkit.getScheduler().cancelTask(runnableMap.get(uuid));
                runnableMap.remove(uuid);
                distanceMap.remove(uuid);
                targetMap.remove(uuid);

                return;
            }

            String loreString = Arrays.toString(item.getItemMeta().getLore().toArray());

            if (loreString.contains("§6Velocity")) {
                velocityMode(p, uuid);
            } else if (loreString.contains("§5Teleport")) {
                teleportMode(p, uuid);
            }
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

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        ItemStack item = e.getItemDrop().getItemStack();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        String loreString = Arrays.toString(lore.toArray());

        if (!meta.getDisplayName().equals("§dGravity Gun")) return;
        if (!loreString.contains("§dMode: ")) return;

        e.setCancelled(true);

        if (runnableMap.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(runnableMap.get(uuid));
            runnableMap.remove(uuid);
            distanceMap.remove(uuid);
            targetMap.remove(uuid);
        }

        if (loreString.contains("§6Velocity")) {
            lore.set(1, "§dMode: §5Teleport");
        } else if (loreString.contains("§5Teleport")) {
            lore.set(1, "§dMode: §6Velocity");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void velocityMode(Player p, UUID uuid) {
        Entity target = getTarget(p);

        if (target == null) return;

        if (target instanceof LivingEntity) {
            distanceMap.put(uuid, p.getEyeLocation().distance(target.getLocation().add(0, ((LivingEntity) target).getEyeHeight(), 0)));
        } else {
            distanceMap.put(uuid, p.getEyeLocation().distance(target.getLocation()));
        }

        targetMap.put(uuid, target.getUniqueId());

        BukkitTask runnable = new BukkitRunnable() {

            private final Entity entity = getEntity(targetMap.get(uuid));

            @Override
            public void run() {
                if (entity == null || !entity.isValid()) {
                    runnableMap.remove(uuid);
                    distanceMap.remove(uuid);
                    targetMap.remove(uuid);
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

    private void teleportMode(Player p, UUID uuid) {
        Entity target = getTarget(p);

        if (target == null) return;

        if (target instanceof LivingEntity) {
            distanceMap.put(uuid, p.getEyeLocation().distance(target.getLocation().add(0, ((LivingEntity) target).getEyeHeight(), 0)));
        } else {
            distanceMap.put(uuid, p.getEyeLocation().distance(target.getLocation()));
        }

        targetMap.put(uuid, target.getUniqueId());

        BukkitTask runnable = new BukkitRunnable() {

            private final Entity entity = getEntity(targetMap.get(uuid));

            @Override
            public void run() {
                if (entity == null || !entity.isValid()) {
                    runnableMap.remove(uuid);
                    distanceMap.remove(uuid);
                    targetMap.remove(uuid);
                    cancel();

                    return;
                }

                Location wanted = p.getEyeLocation().add(p.getLocation().getDirection().multiply(distanceMap.get(uuid)));

                if (entity instanceof LivingEntity) {
                    wanted.subtract(0, ((LivingEntity) entity).getEyeHeight(), 0);
                }

                entity.teleport(wanted);
                entity.setVelocity(new Vector());
            }
        }.runTaskTimer(this, 0, 1);

        runnableMap.put(uuid, runnable.getTaskId());
    }

    private Entity getTarget(Player player) {
        Location eye = player.getEyeLocation();
        Vector eyeVector = eye.toVector();
        Vector eyeDirection = eye.getDirection();
        Entity target = null;

        for (Entity entity : player.getWorld().getEntities()) {
            if (entity == player) continue;

            Vector toEntity;

            if (entity instanceof LivingEntity) {
                toEntity = entity.getLocation().add(0, ((LivingEntity) entity).getEyeHeight(), 0).toVector().subtract(eyeVector);
            } else {
                toEntity = entity.getLocation().toVector().subtract(eyeVector);
            }

            if (toEntity.normalize().dot(eyeDirection) > 0.99D) {
                target = entity;

                break;
            }
        }

        return target;
    }

    private Entity getEntity(UUID uuid) {
        for (World world : Bukkit.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid)) {
                    return entity;
                }
            }
        }

        return null;
    }
}










