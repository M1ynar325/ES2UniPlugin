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

## 其它段

`tax` `tax-report` `bankruptcy` `checkin` `territory` `report` `showcase` `newbie-guide` `broadcast` `lucky-block` `music.presets` `weapons` `audit` `tps` `afk` `commands`

更新 jar 后执行一次 `/ecos reload` 或重启即可。
