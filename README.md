# 鲸元券接接乐 · Whale Ticket Arcade

竖屏安卓接物小游戏。左侧双向摇杆移动，右侧按键跳跃。让鲸元券碰到蓝发角色头顶的钢盆来得分。连续接住会提高每张券的分数，漏掉一张减一颗心。最高分保存在设备上。

## 构建

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 在 `app/build/outputs/apk/debug/app-debug.apk`。

GitHub Actions 在推送 `main` 和 PR 时构建 Debug APK。推送 `v*` 标签后，签名流程用仓库 Secrets 构建并验证 Release APK。两个产物可从对应运行的 **Artifacts** 下载。

| Secret | 内容 |
| --- | --- |
| `KEYSTORE_BASE64` | JKS 文件的 Base64 |
| `KEYSTORE_PASSWORD` | 密钥库密码 |
| `KEY_ALIAS` | 签名密钥别名 |
| `KEY_PASSWORD` | 签名密钥密码 |

## 美术

`app/src/main/assets/` 里的角色和券用内置 ImageGen 根据用户提供的参考图制作。要点是保持蓝色长发、女仆裙、鲸鱼围裙和头顶钢盆，双手自然下垂；走路向左、向右和屈膝跳跃采用独立姿势。顶部角色保留正视镜头、蓝西服、眼镜、严肃表情和手指在嘴唇前的姿势。券保留蓝边、浅色票面、鲸鱼与“鲸元券”字样。舞台、界面与特效使用 Android Canvas 绘制。
