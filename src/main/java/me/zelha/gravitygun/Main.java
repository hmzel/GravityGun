package me.zelha.gravitygun;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
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
    private final Map<UUID, List<UUID>> targetMap = new HashMap<>();
    private final Map<UUID, Map<UUID, Vector>> offsetMap = new HashMap<>();

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
            Bukkit.getScheduler().cancelTask(runnableMap.get(uuid));
            runnableMap.remove(uuid);
            distanceMap.remove(uuid);
            offsetMap.remove(uuid);

            Entity first = getEntity(targetMap.get(uuid).get(0));

            if (first == null || !first.isValid()) {
                targetMap.remove(uuid);

                return;
            }

            Location entityLoc = first.getLocation();

            if (first instanceof LivingEntity) {
                entityLoc = ((LivingEntity) first).getEyeLocation();
            }

            Vector velocity = entityLoc.subtract(p.getEyeLocation()).toVector().normalize().multiply(2.5);

            for (UUID id : targetMap.get(uuid)) {
                Entity entity = getEntity(id);

                if (entity == null || !entity.isValid()) continue;

                entity.setVelocity(velocity);
            }

            targetMap.remove(uuid);
        }

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (runnableMap.containsKey(uuid)) {
                Bukkit.getScheduler().cancelTask(runnableMap.get(uuid));
                runnableMap.remove(uuid);
                distanceMap.remove(uuid);
                targetMap.remove(uuid);
                offsetMap.remove(uuid);

                return;
            }

            String loreString = Arrays.toString(item.getItemMeta().getLore().toArray());

            if (loreString.contains("§6Velocity")) {
                velocityMode(p, uuid);
            } else if (loreString.contains("§5Teleport")) {
                teleportMode(p, uuid);
            } else if (loreString.contains("§9Blocks")) {
                blocksMode(p, uuid, item.getItemMeta().getLore());
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
        String loreString = Arrays.toString(lore.toArray());

        if (!meta.getDisplayName().equals("§dGravity Gun")) return;
        if (!loreString.contains("§dMode: ")) return;
        if (!distanceMap.containsKey(uuid) && !(p.isSneaking() && loreString.contains("§9Blocks"))) return;

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

        if (p.isSneaking() && loreString.contains("§9Blocks")) {
            int area = Integer.parseInt(lore.get(2).replace("§dArea: §b", ""));

            if (area + amount < 1) return;

            lore.set(2, "§dArea: §b" + (area + amount));
            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            distanceMap.put(uuid, distanceMap.get(uuid) + amount);

            if (distanceMap.get(uuid) < 0) {
                distanceMap.put(uuid, 0D);
            }
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
            offsetMap.remove(uuid);
        }

        if (loreString.contains("§6Velocity")) {
            lore.set(1, "§dMode: §5Teleport");
        } else if (loreString.contains("§5Teleport")) {
            lore.set(1, "§dMode: §9Blocks");
            lore.add(2, "§dArea: §b1");
            lore.add("§dShift + Scroll Wheel: Increase area to pick up");
        } else if (loreString.contains("§9Blocks")) {
            lore.set(1, "§dMode: §6Velocity");
            lore.remove(2);
            lore.remove(7);
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

        targetMap.put(uuid, Collections.singletonList(target.getUniqueId()));

        BukkitTask runnable = new BukkitRunnable() {

            private final Entity entity = getEntity(targetMap.get(uuid).get(0));

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

        targetMap.put(uuid, Collections.singletonList(target.getUniqueId()));

        BukkitTask runnable = new BukkitRunnable() {

            private final Entity entity = getEntity(targetMap.get(uuid).get(0));

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

    private void blocksMode(Player p, UUID uuid, List<String> lore) {
        Block block = p.getTargetBlock((Set<Material>) null, 256);
        int area = Integer.parseInt(lore.get(2).replace("§dArea: §b", "")) - 1;

        if (block == null) return;
        if (block.getType() == Material.AIR) return;

        targetMap.put(uuid, new ArrayList<>());
        offsetMap.put(uuid, new HashMap<>());
        distanceMap.put(uuid, p.getEyeLocation().distance(block.getLocation().add(0.5, 0.5, 0.5)));

        for (int x = area; x >= -area; x--) {
            for (int y = area; y >= -area; y--) {
                for (int z = area; z >= -area; z--) {
                    Block target = block.getLocation().add(x, y, z).getBlock();

                    if (target.getType() == Material.AIR) continue;

                    FallingBlock fallingBlock = target.getWorld().spawnFallingBlock(target.getLocation(), target.getType(), target.getData());

                    target.setType(Material.AIR);
                    targetMap.get(uuid).add(fallingBlock.getUniqueId());
                    offsetMap.get(uuid).put(fallingBlock.getUniqueId(), new Vector(x, y, z));
                }
            }
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Location wanted = p.getEyeLocation().add(p.getLocation().getDirection().multiply(distanceMap.get(uuid))).subtract(0, 0.5, 0);

                for (UUID id : new ArrayList<>(targetMap.get(uuid))) {
                    FallingBlock block = (FallingBlock) getEntity(id);

                    if (block == null || !block.isValid()) {
                        targetMap.get(uuid).remove(id);
                        offsetMap.get(uuid).remove(id);

                        continue;
                    }

                    Vector offset = offsetMap.get(uuid).get(block.getUniqueId());

                    block.teleport(wanted.clone().add(offset));
                    block.setVelocity(new Vector());
                    block.setTicksLived(1);
                }
            }
        }.runTaskTimer(this, 0, 1);

        runnableMap.put(uuid, task.getTaskId());
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

            if (toEntity.normalize().dot(eyeDirection) > 0.98D) {
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










