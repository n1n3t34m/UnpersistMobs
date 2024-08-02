package org.nineteam.unpersistMobs;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.entity.Mob;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;

public final class UnpersistMobs extends JavaPlugin implements Listener, CommandExecutor {
    NamespacedKey unpersist = new NamespacedKey(this, "unpersist");
    FileConfiguration config = getConfig();
    int markedCounter, despawnedCounter = 0;

    @Override
    public void onEnable() {
        config.addDefault("ticksLived", 1728000);
        config.addDefault("logOnly", false);
        config.addDefault("dropItems", true);
        config.options().copyDefaults(true);
        saveConfig();

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("admin").setExecutor(this);
    }

    /*@Override
    public void onDisable() {
    }*/

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length > 0) {
            switch (args[0]) {
                case "reload":
                    reloadConfig();  // Discard
                    config = getConfig();  // Update
                    sender.sendMessage("Configuration reloaded");
                    return true;
                case "stats":
                    sender.sendMessage(String.format("Mobs marked: %d (and %d already despawned)",
                            markedCounter, despawnedCounter));
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    private void log(String verb, Mob mob) {
        Location loc = mob.getLocation();
        getLogger().info(String.format("%s with uuid %s in %s at %.1f %.1f %.1f %s %.1f hours!",
                mob.getName(), mob.getUniqueId(),
                loc.getWorld().getName(), loc.x(), loc.y(), loc.z(),
                verb, (float)mob.getTicksLived() / 20 / 3600)
        );
    }

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        SpawnReason reason = entity.getEntitySpawnReason();
        // Naturally spawned monsters which lived too long without a name tag
        // NOTE: Custom name will set RemoveWhenFarAway to false, but sitting in a vehicle won't
        if(!(reason.equals(SpawnReason.DEFAULT) || reason.equals(SpawnReason.CUSTOM))
                && entity.getSpawnCategory().equals(SpawnCategory.MONSTER) && entity.customName() == null
                && entity.getTicksLived() > config.getInt("ticksLived")) {
            Mob mob = (Mob)entity;
            // Mobs which can pick up items and have RemoveWhenFarAway false
            //  so assume mob already picked up some items and persistent because of it
            if(mob.getCanPickupItems() && !mob.getRemoveWhenFarAway()) {
                if(config.getBoolean("logOnly")) {
                    log("persisted for", mob);
                } else {
                    // Set mob for despawn and mark for items drop
                    mob.setRemoveWhenFarAway(true);
                    mob.getPersistentDataContainer().set(unpersist, PersistentDataType.BOOLEAN, true);
                    markedCounter++;
                    log("marked after", mob);
                }
            }
        }
    }

    @EventHandler
    public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        // Detect despawn of marked mob
        // FIXME: Must be replaced with event cause in the future
        if(entity.getPersistentDataContainer().getOrDefault(unpersist, PersistentDataType.BOOLEAN, false)
                && entity.isDead() && entity.getChunk().isLoaded()) {
            Mob mob = (Mob)entity;
            if(config.getBoolean("dropItems")) {
                // Get items
                EntityEquipment equipment = mob.getEquipment();
                ItemStack[] armor = equipment.getArmorContents();
                ItemStack mainHand = equipment.getItemInMainHand();
                ItemStack offHand = equipment.getItemInOffHand();

                // Drop items
                World world = mob.getWorld();
                Location loc = mob.getLocation();
                for (ItemStack armorPiece : armor) {
                    world.dropItemNaturally(loc, armorPiece);
                }
                world.dropItemNaturally(loc, mainHand);
                world.dropItemNaturally(loc, offHand);
            }
            despawnedCounter++;
            log("despawned after", mob);
        }
    }
}
