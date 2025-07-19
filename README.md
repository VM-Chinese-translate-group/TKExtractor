# Translation Key Extractor (翻译键提取器)

[![许可证: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://www.minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.1+-blue.svg)](https://neoforged.net/)

一款面向整合包开发者与本地化工作者的专业工具，致力于解决因物品显示名称被硬编码于源码、却未提供标准语言文件而导致的国际化难题。

---

## 项目背景与痛点解析

在模组开发或使用脚本（如KubeJS）添加自定义内容时，开发者有时会通过代码直接指定物品的显示名称（例如 `displayName("Example Item")`），而不是遵循标准的国际化（i18n）流程，即在语言文件（如 `en_us.json`）中定义翻译键。

对于英语环境下的用户而言，这些物品显示正常。然而，这为**本地化工作（翻译）带来了巨大障碍**。当翻译者试图为这些内容创建其他语言的本地化文件（如 `zh_cn.json`）时，他们会发现无法找到与之对应的翻译键（例如 `item.modid.example_item`）。若无此键，便无法为其添加翻译。

传统上，翻译者必须通过经验猜测、查阅源码、反编译或借助调试工具来逆向查找这些翻译键，这一过程不仅效率低下，且极易出错。本工具旨在彻底解决这一难题。

## 本工具的解决方案

**翻译键提取器 (Translation Key Extractor)** 将翻译键的发现过程完全自动化。它通过以下方式工作：

1.  **深度扫描**: 启动后，工具会遍历游戏内所有已注册的内容实体。
2.  **智能映射**: 它会精确识别出那些显示名称由程序硬编码、且具有实际文本内容的条目。
3.  **数据导出**: 工具会将这些**翻译键**与其对应的**默认显示名称**（通常为英文）之间的映射关系，导出为一个结构清晰、格式规范的JSON文件。

这份导出的文件为本地化工作者提供了至关重要的参考依据，使其能够准确无误地为每一个条目创建翻译。

## 功能特性

-   **全面扫描**: 支持对原版及模组内容的全面扫描，包括物品、方块、流体、实体，以及第三方模组的自定义注册表（如Mekanism的化学物质）。
-   **精准提取**: 精准识别显示名称经由代码设置且不同于其翻译键的条目，确保导出的数据均为有效信息。
-   **便捷输出**: 在游戏根目录下的 `tkextractor_output` 文件夹中生成格式规范的JSON文件，为本地化工作者提供清晰的参考。
-   **路径直达**: 任务完成后，在游戏内聊天框提供可点击的链接，以便即时访问输出目录，优化工作流。
-   **高度兼容**: 采用软依赖方式兼容Mekanism等第三方模组，无需强制安装即可在检测到其存在时扩展扫描范围。

## 使用指南

本模组提供了一条服务器管理员指令（需2级以上权限）。

### 指令格式

```
/tkextractor <命名空间|_all_>
```

### 参数详解

-   `<命名空间>`: 指定需要扫描的目标命名空间，如 `kubejs`, `minecraft`。
-   `_all_`: 用于扫描所有已加载的命名空间，并将结果整合输出。

### 应用示例

假设一个KubeJS脚本创建了一个物品，其翻译键为 `item.kubejs.unobtainium_gear`，并硬编码了其显示名称为 "Unobtainium Gear"。

1.  执行指令以扫描`kubejs`命名空间：
    ```
    /tkextractor kubejs
    ```
2.  在 `tkextractor_output` 文件夹内找到 `kubejs.json` 文件。
3.  文件内容如下：
    ```json
    {
      "item.kubejs.unobtainium_gear": "Unobtainium Gear"
    }
    ```
4.  本地化工作者现在可以依据此信息，在 `zh_cn.json` 文件中添加翻译：
    ```json
    {
      "item.kubejs.unobtainium_gear": "源质齿轮"
    }
    ```

## 许可证

本项目基于 **GNU Affero General Public License v3.0 (AGPL-3.0)** 许可证授权。这意味着如果您在公共网络环境中（如服务器）使用或分发本软件的修改版本，您必须以相同的许可证公开您的修改后源码。详细条款请参阅`LICENSE`文件。