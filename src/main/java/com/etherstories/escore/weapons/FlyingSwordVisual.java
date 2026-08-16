package com.etherstories.escore.weapons;

import com.etherstories.escore.managers.WeaponManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * 飞剑视觉：ItemDisplay 剑模型，比盔甲架手臂自然得多。
 */
public final class FlyingSwordVisual {

    private final ItemDisplay display;
    private final WeaponManager wm;

    private FlyingSwordVisual(ItemDisplay display, WeaponManager wm) {
        this.display = display;
        this.wm = wm;
    }

    public static FlyingSwordVisual spawn(Location loc, WeaponManager wm, boolean tipDown) {
        ItemDisplay d = loc.getWorld().spawn(loc, ItemDisplay.class, disp -> {
            disp.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
            disp.setBillboard(Display.Billboard.FIXED);
            disp.setBrightness(new Display.Brightness(15, 15));
            disp.setShadowRadius(0f);
            disp.setShadowStrength(0f);
            try {
                disp.setTeleportDuration(1);
                disp.setInterpolationDuration(1);
            } catch (Throwable ignored) {
            }
            // 默认物品朝向偏平；旋转成「剑身沿视线前进」
            float pitch = tipDown ? (float) Math.toRadians(90) : (float) Math.toRadians(-90);
            disp.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(pitch, 1f, 0f, 0f),
                    new Vector3f(0.85f, 0.85f, 0.85f),
                    new AxisAngle4f(0f, 0f, 1f, 0f)
            ));
            disp.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
        });
        wm.registerEntity(d);
        return new FlyingSwordVisual(d, wm);
    }

    public boolean dead() {
        return display.isDead() || !display.isValid();
    }

    public Location getLocation() {
        return display.getLocation();
    }

    public void teleport(Location loc) {
        display.teleport(loc);
    }

    /** 环绕悬浮：平滑位移，使用传入朝向 */
    public void hoverTo(Location loc) {
        try {
            display.setTeleportDuration(3);
        } catch (Throwable ignored) {
        }
        display.teleport(loc);
    }

    /** 移动并让剑尖大致朝向 velocity 方向 */
    public void move(Location loc, Vector dir) {
        Location l = loc.clone();
        if (dir != null && dir.lengthSquared() > 1e-6) {
            Vector n = dir.clone().normalize();
            l.setDirection(n);
            if (n.getY() < -0.3) {
                l.setPitch(Math.min(90f, l.getPitch() + 25f));
            }
        }
        try {
            display.setTeleportDuration(1);
        } catch (Throwable ignored) {
        }
        display.teleport(l);
    }

    public void remove() {
        if (!display.isDead()) display.remove();
        wm.unregisterEntity(display);
    }

    public Entity entity() {
        return display;
    }
}
