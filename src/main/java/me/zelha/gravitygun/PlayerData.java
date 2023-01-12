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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class PlayerData {

    private final BukkitTask runnable;
    private final UUID player;
    private final List<UUID> targetList = new ArrayList<>();
    private final Map<UUID, Vector> offsetMap = new HashMap<>();
    private final Mode mode;
    private double distance = 0;

    public PlayerData(Player player, Mode mode, List<String> lore) {
        this.player = player.getUniqueId();

        if (mode != Mode.BLOCKS) {
            Vector eyeVector = player.getEyeLocation().toVector();
            Vector eyeDirection = player.getEyeLocation().getDirection();
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

            if (target == null) {
                this.mode = null;
                this.runnable = null;

                stop(false);

                return;
            }

            if (target instanceof LivingEntity) {
                distance = player.getEyeLocation().distance(target.getLocation().add(0, ((LivingEntity) target).getEyeHeight(), 0));
            } else {
                distance = player.getEyeLocation().distance(target.getLocation());
            }

            targetList.add(target.getUniqueId());
        } else {
            Block block = player.getTargetBlock((Set<Material>) null, 256);
            int area = Integer.parseInt(lore.get(2).replace("§dArea: §b", "")) - 1;

            if (block == null || block.getType() == Material.AIR) {
                this.mode = null;
                this.runnable = null;

                stop(false);

                return;
            }

            distance = player.getEyeLocation().distance(block.getLocation().add(0.5, 0.5, 0.5));

            for (int x = area; x >= -area; x--) {
                for (int y = area; y >= -area; y--) {
                    for (int z = area; z >= -area; z--) {
                        Block target = block.getLocation().add(x, y, z).getBlock();

                        if (target.getType() == Material.AIR) continue;

                        FallingBlock fallingBlock = target.getWorld().spawnFallingBlock(target.getLocation(), target.getType(), target.getData());

                        target.setType(Material.AIR);
                        targetList.add(fallingBlock.getUniqueId());
                        offsetMap.put(fallingBlock.getUniqueId(), new Vector(x, y, z));
                    }
                }
            }
        }

        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Location wanted = player.getEyeLocation().add(player.getLocation().getDirection().multiply(distance));

                for (UUID id : new ArrayList<>(targetList)) {
                    Entity entity = getEntity(id);
                    Location l = wanted.clone();

                    if (entity == null || !entity.isValid()) {
                        if (mode != Mode.BLOCKS) {
                            stop(false);
                        } else {
                            targetList.remove(id);
                        }

                        continue;
                    }

                    if (entity instanceof LivingEntity) {
                        l.subtract(0, ((LivingEntity) entity).getEyeHeight(), 0);
                    } else if (entity instanceof FallingBlock) {
                        l.subtract(0, 0.5, 0);
                    }

                    if (mode == Mode.BLOCKS) {
                        l.add(offsetMap.get(entity.getUniqueId()));
                    }

                    if (mode == Mode.VELOCITY) {
                        entity.setVelocity(l.subtract(entity.getLocation()).toVector());
                        entity.setFallDistance(0);
                    } else {
                        entity.teleport(l);
                        entity.setVelocity(new Vector());

                        if (mode == Mode.BLOCKS) {
                            entity.setTicksLived(1);
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 1);

        this.mode = mode;
    }

    public void stop(boolean shouldThrow) {
        Main.getInstance().removeData(player);

        if (runnable != null) {
            runnable.cancel();
        }

        if (shouldThrow) {
            Player p = Bukkit.getPlayer(player);
            Entity first = getEntity(targetList.get(0));

            if (p == null || !p.isValid() || !p.isOnline()) return;
            if (first == null || !first.isValid()) return;

            Location entityLoc = first.getLocation();

            if (first instanceof LivingEntity) {
                entityLoc = ((LivingEntity) first).getEyeLocation();
            }

            Vector velocity = entityLoc.subtract(p.getEyeLocation()).toVector().normalize().multiply(2.5);

            for (UUID id : targetList) {
                Entity entity = getEntity(id);

                if (entity == null || !entity.isValid()) continue;

                entity.setVelocity(velocity);
            }
        } else if (mode == Mode.BLOCKS) {
            for (UUID id : new ArrayList<>(targetList)) {
                FallingBlock block = (FallingBlock) getEntity(id);

                if (block == null || !block.isValid()) return;

                block.getLocation().getBlock().setType(block.getMaterial());
                block.remove();
            }
        }
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

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        if (distance < 0) {
            distance = 0;
        }

        this.distance = distance;
    }
}
