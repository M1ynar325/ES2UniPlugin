package com.etherstories.escore.weapons;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/** 玩家技能 PDC：霁弧 grant 后潜行+F；也可绑武器右键。 */
public final class PlayerSkills {

    private static NamespacedKey gleamArcKey;

    private PlayerSkills() {}

    public static void init(ES2UniPlugin plugin) {
        gleamArcKey = new NamespacedKey(plugin, "skill_gleam_arc");
    }

    public static boolean hasGleamArc(Player player) {
        Byte v = player.getPersistentDataContainer().get(gleamArcKey, PersistentDataType.BYTE);
        return v != null && v != 0;
    }

    public static void setGleamArc(Player player, boolean on) {
        if (on) {
            player.getPersistentDataContainer().set(gleamArcKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            player.getPersistentDataContainer().remove(gleamArcKey);
        }
    }
}
