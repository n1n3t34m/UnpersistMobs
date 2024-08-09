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
import org.bukkit.event.EventPriority;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class UnpersistMobs extends JavaPlugin implements Listener, CommandExecutor {
    NamespacedKey unpersist = new NamespacedKey(this, "unpersist");
    FileConfiguration config = getConfig();
    int markedCounter, despawnedCounter, droppedCounter = 0;

    @Override
    public void onEnable() {
        config.addDefault("ticksLived", 1728000);
        config.addDefault("logOnly", false);
        config.addDefault("dropItems", true);
        config.addDefault("ignoredSpawnReasons", List.of(
                "DEFAULT", "CUSTOM", "COMMAND"));  // Only natural spawns
        config.options().copyDefaults(true);
        saveConfig();

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("reload").setExecutor(this);
        getCommand("stats").setExecutor(this);
    }

    /*@Override
    public void onDisable() {
    }*/

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName()) {
            case "reload":
                reloadConfig();  // Discard
                config = getConfig();  // Update
                sender.sendMessage("Configuration reloaded");
                return true;
            case "stats":
                sender.sendMessage(String.format("Mobs marked: %d\nMobs despawned: %d\nItems dropped: %d",
                        markedCounter, despawnedCounter, droppedCounter));
                return true;
            default:
                return false;
        }
    }

    private void log(String verb, Mob entity) {
        Location loc = entity.getLocation();
        getLogger().info(String.format("%s %s uuid %s in %s at %.1f %.1f %.1f %s %.1f hours!",
                entity.getEntitySpawnReason(), entity.getType(), entity.getUniqueId(),
                loc.getWorld().getName(), loc.x(), loc.y(), loc.z(),
                verb, (float) entity.getTicksLived() / 20 / 3600)
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.isValid() && entity.getSpawnCategory().equals(SpawnCategory.MONSTER)) {  // Only monsters
            SpawnReason reason = entity.getEntitySpawnReason();
            if (!config.getStringList("ignoredSpawnReasons").contains(reason.toString())) {  // Filter reason
                Mob mob = (Mob) entity;
                if (!mob.getRemoveWhenFarAway() && mob.customName() == null  // Persistent without nametag
                        && mob.getTicksLived() > config.getInt("ticksLived")) {  // Lived for too long
                    if (config.getBoolean("logOnly")) {
                        log("persisted for", mob);
                    } else {
                        // Set mob for despawn and mark
                        mob.setRemoveWhenFarAway(true);
                        mob.getPersistentDataContainer().set(unpersist, PersistentDataType.BOOLEAN, true);
                        markedCounter++;
                        log("marked after", mob);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.isDead() && entity.getChunk().isLoaded()  // FIXME: Replace with event cause in the future
                && entity.getPersistentDataContainer().getOrDefault(unpersist, PersistentDataType.BOOLEAN, false)) {
            Mob mob = (Mob) entity;
            if (mob.getCanPickupItems() && config.getBoolean("dropItems")) {  // Entity may picked up something
                List<ItemStack> items = new ArrayList<>();

                // Get items
                EntityEquipment equipment = mob.getEquipment();
                items.add(equipment.getItemInMainHand());
                items.add(equipment.getItemInOffHand());
                items.addAll(Arrays.asList(equipment.getArmorContents()));

                // Drop items
                World world = mob.getWorld();
                Location loc = mob.getLocation();
                for (ItemStack item : items) {
                    if (!item.getType().isEmpty()) {
                        droppedCounter++;
                        world.dropItemNaturally(loc, item);
                    }
                }
            }
            despawnedCounter++;
            log("despawned after", mob);
        }
    }
}
