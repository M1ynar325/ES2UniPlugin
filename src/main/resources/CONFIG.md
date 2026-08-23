# ES2UniPlugin 配置说明（自动生成/更新）

本文件由插件在启动或 `/ecos reload` 时从内置模板同步到  
`plugins/ES2UniPlugin/CONFIG.md`（`config.auto-update-docs: true`）。

**不会覆盖** 你改过的 `config.yml` 数值；只会：
1. 给 `config.yml` **补全新键**（`config.auto-fill-missing: true`）
2. 同步 `/ecos help` 文案（`commands.auto-update-help: true`）
3. 刷新本说明文档（`config.auto-update-docs: true`）

关掉某一项：在 `config.yml` 对应键设为 `false`。

---

## 市政管理处（1.14+）

ECOS 终端 → **管理处**，或 `/ecos municipal`。

| 服务 | 说明 |
|------|------|
| 招工处 | 个人/官方建设委托；托管报酬；完成后打款 |
| 垃圾清理 | 投票清理；掉落物进回收站 |
| 回收站 | 取回自己的清理掉落物，默认保存 24h |
| 保险柜 | 免费背包清空险；理赔需管理员批准 |

负债：`bankruptcy.reserve: -500`（低于不可消费）；负余额消费会通知管理。

配置段：`municipal.*` · 数据：`jobs.yml` · `insurance.yml` · `recyclebin.yml`

---

## 版本 / 自维护

| 键 | 说明 |
|----|------|
| `config-version` | 勿手改 |
| `config.auto-fill-missing` | 深度补缺键 |
| `config.auto-update-docs` | 写出本 CONFIG.md |

## 房产（1.20+）

和领地分开：领地是 2D 地皮保护。房产是按规定登记的 3D 房间（买卖 / 传送）。**没按规定注册的房子出问题不保障；注册成房产才受保障。**

ECOS 终端 → **出行 / 城市** → 房产，或 `/ecos estate`。

分类：住宅 `residential` · 公共建筑 `public` · 商业建筑 `commercial`。  
用途：house 住宅 / apartment 公寓 / shop 商铺 / workshop 工坊 / studio 工作室 / storage 仓库 / other 其他。

玩家自建住宅/公寓收注册费 `estate.register-fee`（默认 2048），圈两点后登记，默认不上架。管理 `/ecos estate set` 预制不收费。牌子：`/ecos estate sign` 写「ES2注册单位」。

看房：终端 **买房** 页左键传到门口，右键两次购买。`/ecos estate here` 看当前门牌。`/ecos estate visit <楼> <层> <号>`。登记、门口、牌子在终端「登记房产」。

配置：`estate.max-volume`。数据：`estates.yml`。

## 其它段

`tax` `tax-report` `bankruptcy` `checkin` `territory` `estate` `report` `showcase` `newbie-guide` `broadcast` `lucky-block` `music.presets` `weapons` `audit` `tps` `afk` `commands`

## 霁弧 Gleam Arc

可绑武器：`/ecos weapon bind gleam_arc <right|sneak_right>`（和回声散射一样右键放）。  
也可 `/ecos skill grant <玩家> gleam_arc`，潜行+F 释放（普通 F 仍换副手）。冷却 3 秒。配置：`weapons.skills.gleam_arc`。

更新 jar 后执行一次 `/ecos reload` 或重启即可。
