package com.example.punchsnowball;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public final class PunchSnowballPlugin extends JavaPlugin implements Listener {
    private NamespacedKey snowballKey;
    private NamespacedKey tntKey;
    private NamespacedKey tntOwnerKey;

    // All values can be changed in plugins/PunchSnowball/config.yml.
    private int snowballCooldownTicks;
    private int slownessTicks;
    private double snowballHorizontal;
    private double snowballVertical;
    private int tntFuseTicks;
    private double tntRadius;
    private double tntDamage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        snowballCooldownTicks = Math.max(1, getConfig().getInt("snowball.cooldown-seconds", 7) * 20);
        slownessTicks = Math.max(1, getConfig().getInt("snowball.slowness-seconds", 2) * 20);
        snowballHorizontal = Math.max(0.0, getConfig().getDouble("snowball.horizontal-velocity", 2.45));
        snowballVertical = Math.max(0.0, getConfig().getDouble("snowball.vertical-velocity", 0.40));
        tntFuseTicks = Math.max(1, getConfig().getInt("tnt.fuse-seconds", 3) * 20);
        tntRadius = Math.max(0.5, getConfig().getDouble("tnt.radius-blocks", 2.75));
        tntDamage = Math.max(0.0, getConfig().getDouble("tnt.damage-health-points", 4.0));

        snowballKey = new NamespacedKey(this, "punch_snowball");
        tntKey = new NamespacedKey(this, "infinite_tnt");
        tntOwnerKey = new NamespacedKey(this, "tnt_owner");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private ItemStack makeSnowball() {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Punch Snowball");
        meta.setLore(List.of(
                ChatColor.GRAY + "Infinite throws",
                ChatColor.GRAY + (snowballCooldownTicks / 20) + "-second cooldown",
                ChatColor.LIGHT_PURPLE + "Extra-strong knockback + Slowness II"));
        meta.getPersistentDataContainer().set(snowballKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeTnt() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Infinite TNT");
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click a block to ignite",
                ChatColor.GRAY + "Reusable - never consumed",
                ChatColor.YELLOW + (tntFuseTicks / 20) + "-second fuse / small blast",
                ChatColor.RED + "Approx. 2 hearts of damage"));
        // Real enchantment shimmer, no gameplay enchantment necessary.
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(tntKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isSpecial(ItemStack item, Material expected, NamespacedKey key) {
        return item != null && item.getType() == expected && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                .has(key, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onThrow(PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof Snowball snowball)) return;
        if (!isSpecial(event.getItemStack(), Material.SNOWBALL, snowballKey)) return;

        Player player = event.getPlayer();
        if (player.hasCooldown(Material.SNOWBALL)) {
            event.setCancelled(true);
            return;
        }
        snowball.getPersistentDataContainer().set(snowballKey, PersistentDataType.BYTE, (byte) 1);
        event.setShouldConsume(false);
        player.setCooldown(Material.SNOWBALL, snowballCooldownTicks);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball)) return;
        if (!projectile.getPersistentDataContainer().has(snowballKey, PersistentDataType.BYTE)) return;
        if (!(event.getHitEntity() instanceof Player target)) return;
        if (projectile.getShooter() instanceof Player shooter && shooter.equals(target)) return;

        Vector direction = projectile.getVelocity().clone().setY(0);
        if (direction.lengthSquared() < 0.0001 && projectile.getShooter() instanceof Player shooter) {
            direction = target.getLocation().toVector()
                    .subtract(shooter.getLocation().toVector()).setY(0);
        }
        if (direction.lengthSquared() < 0.0001) direction = new Vector(0, 0, 1);

        // Direct velocity is much stronger than normal knockback, including on
        // players wearing knockback-resistant netherite armor.
        direction.normalize().multiply(snowballHorizontal).setY(snowballVertical);
        target.setVelocity(direction);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                slownessTicks, 1, false, true, true));
    }

    @EventHandler
    public void onPlaceTnt(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!isSpecial(event.getItem(), Material.TNT, tntKey)) return;

        // Stop the actual TNT block from being placed/consumed.
        event.setCancelled(true);
        Location spot = event.getClickedBlock().getRelative(event.getBlockFace())
                .getLocation().add(0.5, 0.0, 0.5);
        if (!spot.getBlock().getType().isAir()) {
            event.getPlayer().sendMessage(ChatColor.RED + "No room to place Infinite TNT there.");
            return;
        }

        Player placer = event.getPlayer();
        TNTPrimed primed = spot.getWorld().spawn(spot, TNTPrimed.class);
        primed.setSource(placer);
        // A few extra ticks are a safety buffer: the scheduled effect occurs
        // at the exact configured fuse duration and removes the primed TNT.
        primed.setFuseTicks(tntFuseTicks + 5);
        primed.getPersistentDataContainer().set(tntKey, PersistentDataType.BYTE, (byte) 1);
        primed.getPersistentDataContainer().set(tntOwnerKey, PersistentDataType.STRING,
                placer.getUniqueId().toString());

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!primed.isValid()) return;
            detonate(primed);
        }, tntFuseTicks);
    }

    // Safety net if another plugin changes the fuse or explosion timing.
    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed primed)) return;
        if (!primed.getPersistentDataContainer().has(tntKey, PersistentDataType.BYTE)) return;
        event.setCancelled(true); // no vanilla terrain damage or vanilla blast damage
        detonate(primed);
    }

    private void detonate(TNTPrimed primed) {
        Location center = primed.getLocation().add(0, 0.4, 0);
        String ownerId = primed.getPersistentDataContainer()
                .get(tntOwnerKey, PersistentDataType.STRING);
        Player owner = null;
        if (ownerId != null) {
            try { owner = Bukkit.getPlayer(UUID.fromString(ownerId)); }
            catch (IllegalArgumentException ignored) { }
        }
        primed.remove();

        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 4,
                0.3, 0.3, 0.3, 0.0);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);

        DamageSource.Builder damageBuilder = DamageSource.builder(DamageType.MAGIC)
                .withDamageLocation(center);
        if (owner != null) damageBuilder.withCausingEntity(owner);
        DamageSource damageSource = damageBuilder.build();

        for (Entity nearby : center.getWorld().getNearbyEntities(center, tntRadius, tntRadius, tntRadius)) {
            if (!(nearby instanceof LivingEntity victim) || victim.isDead()) continue;
            // Use actual distance, not just the bounding cube.
            if (victim.getLocation().add(0, victim.getHeight() / 2, 0)
                    .distanceSquared(center) > tntRadius * tntRadius) continue;

            // Magic damage ignores netherite's armor reduction, but still
            // respects damage events, protection enchantments and absorption.
            victim.damage(tntDamage, damageSource);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean snow = command.getName().equalsIgnoreCase("givesnowball");
        boolean tnt = command.getName().equalsIgnoreCase("givetnt");
        if (!snow && !tnt) return false;
        if (args.length > 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " [player]");
            return true;
        }

        Player target;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(ChatColor.RED + "Console usage: /" + label + " <player>");
            return true;
        }

        ItemStack item = snow ? makeSnowball() : makeTnt();
        target.getInventory().addItem(item).values().forEach(
                leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
        sender.sendMessage(ChatColor.GREEN + "Gave " + (snow ? "Punch Snowball" : "Infinite TNT")
                + " to " + target.getName());
        return true;
    }
}
