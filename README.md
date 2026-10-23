# K218Launcher

> **特别提示**：本项目仅供本人自用，仅适用于 2019–2021 款雪佛兰科鲁泽原装 Android 车机，未在任何其它机型上测试。

**它是什么**　一个自用的修改版车机桌面，适用于 19–21 款雪佛兰科鲁泽原车机，可直接覆盖安装原厂桌面。

**它的作用**　通过额外手段辅助 DiPlay 正常运行，以替代原厂 CarPlay。

## 什么是 DiPlay

DiPlay 是独立的 CarPlay 接收端（有线 / 无线），项目主页：

https://github.com/shihabal3amri/DiPlay

## 为什么要用 DiPlay 替代原厂 CarPlay

| | 原厂 CarPlay | DiPlay |
|---|---|---|
| 更新 | 固化，无法更新 | 可以更新 |
| 刷新率 | 固定 30 FPS | 最高 60 FPS |
| 触控 | 单点触控，不支持手势，不能滑动 | 多点触控，支持任意手势与滑动 |
| 设置 | 无 | 提供视频流畅度等多项可调设置 |

## 直接安装 DiPlay 存在的问题

- **无法自启动**：车机进入 RST 休眠时会强制停止**所有应用**，DiPlay 在此车机上无法自行启动；
- **无法打开热点**：普通应用无法控制系统热点，每次都需要手动开启。

因此，DiPlay 无法自动连接 iPhone。

## 这个桌面修改了什么

1. **DiPlay 守护线程**：自动保活 DiPlay，并在 RST 休眠后自动拉起；可设置拉起延迟，避免热点晚于 DiPlay 就绪而导致连接中断；
2. **WiFi AP State 守护**：该厂商参数置为 `on` 时，系统会自动保持热点开启；
3. **全部应用列表**：可直接查看、启动、卸载应用，并可添加到桌面；
4. **DiPlay 常驻图标**：一键打开 DiPlay。

## 最终效果

让 DiPlay 与热点始终保持运行，使 DiPlay 能够完整替代原厂 CarPlay。

## 安装

本项目使用 **AOSP 公开平台密钥**签名，与车机原厂桌面证书一致，因此可以直接覆盖安装原厂桌面，**数据保留**。

1. 从 [Releases](../../releases/tag/build) 下载 `K218Launcher-patched.apk`；
2. 手机连接车机热点（网关地址通常为 `192.168.43.1`），确认 adb 可连：

   ```sh
   adb connect 192.168.43.1:5555
   adb devices
   ```

3. 覆盖安装：

   ```sh
   adb install -r K218Launcher-patched.apk
   ```

安装后首次启动会自动完成初始化：补齐三个入口（全部应用 / 守护设置 / 打开DiPlay）、按需重建桌面图标、校正内部计数。

> 注意：两个守护功能默认是**关闭**的，需要在「守护设置」里手动开启。

## 回退

```sh
adb shell pm uninstall com.tricheer.launcher218
```

该命令会卸载本次更新，桌面回到 `/system` 中的原厂版本。

**注意**：卸载会同时清除应用数据（常驻配置、桌面图标顺序、守护设置）。如需彻底恢复，可直接恢复出厂设置。

## 构建

本仓库通过 GitHub Actions 自动构建，推送 `main` 触发（`.mp4` / `.md` / `LICENSE` 的改动不会触发）。

产物发布到 Release（固定 tag `build`），下载地址恒定：

```
https://github.com/Bilx-ZL/DiPlayLauncherForChevroletMonza/releases/download/build/K218Launcher-patched.apk
```

构建流程：apktool 解包原厂 APK → 注入 smali 挂钩 → `javac` + `d8` 生成 `classes2.dex` → 重打包 → `zipalign` → `apksigner`（平台密钥，v1 + v2）签名。

## 免责声明

非官方修改，与雪佛兰、DiPlay 及其它相关厂商均无关联。安装或使用本项目的风险由使用者自行承担，可能包括车机异常、数据丢失等；请在确保安全的环境下操作，**行车过程中请勿操作本界面**。

新增的功能不联网、不上传、不收集任何数据，仅在本机读写系统设置项 `wifi_ap_state` 与自身配置文件，数据全部保留在车机本地。原厂桌面原有的权限与功能未作改动。

## 许可

[MIT](LICENSE)
