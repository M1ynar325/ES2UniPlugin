package com.etherstories.escore.weapons;

import java.util.UUID;

/** 每个活跃技能的生命周期接口，由 WeaponManager 的 master task 驱动。 */
public interface SkillInstance {
    /** 每 tick 调用一次。返回 false 表示技能已结束，可移除。 */
    boolean tick();

    /** 强制清理全部实体和资源（插件卸载 / 玩家退出时调用）。 */
    void cleanup();

    /** 技能归属的玩家 UUID，用于按玩家取消。 */
    UUID getOwnerUUID();
}
