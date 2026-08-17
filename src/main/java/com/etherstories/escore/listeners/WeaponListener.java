package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.items.SkillBinder;
import com.etherstories.escore.weapons.PlayerSkills;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 特殊武器右键触发。对空气必须可用；仅真正会打开 GUI 的方块才让路。
 */
public class WeaponListener implements Listener {

    private final ES2UniPlugin plugin;

    public WeaponListener(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        EquipmentSlot hand = event.getHand();
        // 部分客户端/桥接对 AIR 可能 hand=null；只排除明确的副手
        if (hand == EquipmentSlot.OFF_HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            item = player.getInventory().getItemInMainHand();
        }
        if (item == null || item.getType().isAir()) return;

        if (plugin.getWeaponManager().isCharging(player.getUniqueId())) {
            if (SkillBinder.hasAnySkill(item)) {
                event.setCancelled(true);
                plugin.getWeaponManager().cancelCharge(player.getUniqueId());
            }
            return;
        }

        if (!SkillBinder.hasAnySkill(item)) return;

        // 只有会打开容器/GUI 的方块才不抢技能；楼梯/栅栏/告示等一律当空气打
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && isGuiBlock(block) && !player.isSneaking()) {
                return;
            }
        }

        boolean sneak = player.isSneaking();
        SkillBinder.Slot slot = sneak ? SkillBinder.Slot.SNEAK_RIGHT : SkillBinder.Slot.RIGHT;
        SkillType skill = SkillBinder.getSkill(item, slot);
        if (skill == null) {
            // 蹲下没绑技能时回退到普通右键技能
            if (sneak) skill = SkillBinder.getSkill(item, SkillBinder.Slot.RIGHT);
            if (skill == null) return;
        }

        event.setCancelled(true);
        plugin.getWeaponManager().startCharge(player, skill);
    }

    /** 点击实体时不再误取消蓄力（飞剑/展示实体会挡视线） */
    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        // no-op：蓄力改由再右键 / 受伤 / 换手取消
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) {
            plugin.getWeaponManager().cancelCharge(p.getUniqueId());
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        plugin.getWeaponManager().cancelCharge(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        plugin.getWeaponManager().cancelCharge(event.getPlayer().getUniqueId());
    }

    /** 潜行+F：霁弧。普通 F 仍换副手。 */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking() || !PlayerSkills.hasGleamArc(player)) return;
        if (player.isDead() || !player.isOnline()) return;
        event.setCancelled(true);
        plugin.getWeaponManager().startCharge(player, SkillType.GLEAM_ARC);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getWeaponManager().onPlayerQuit(event.getPlayer().getUniqueId());
    }

    private boolean isGuiBlock(Block block) {
        Material t = block.getType();
        if (t == Material.CRAFTING_TABLE || t == Material.ENCHANTING_TABLE
                || t == Material.ANVIL || t == Material.CHIPPED_ANVIL || t == Material.DAMAGED_ANVIL
                || t == Material.GRINDSTONE || t == Material.STONECUTTER
                || t == Material.LOOM || t == Material.SMITHING_TABLE
                || t == Material.CARTOGRAPHY_TABLE || t == Material.BREWING_STAND
                || t == Material.LECTERN || t == Material.BEACON
                || t == Material.CHEST || t == Material.TRAPPED_CHEST
                || t == Material.ENDER_CHEST || t == Material.BARREL
                || t == Material.FURNACE || t == Material.BLAST_FURNACE || t == Material.SMOKER
                || t == Material.HOPPER || t == Material.DROPPER || t == Material.DISPENSER
                || t.name().endsWith("_SHULKER_BOX") || t == Material.SHULKER_BOX) {
            return true;
        }
        try {
            if (Tag.BUTTONS.isTagged(t) || Tag.DOORS.isTagged(t)
                    || Tag.TRAPDOORS.isTagged(t) || Tag.FENCE_GATES.isTagged(t)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return t == Material.LEVER || t == Material.REPEATER || t == Material.COMPARATOR;
    }
}
