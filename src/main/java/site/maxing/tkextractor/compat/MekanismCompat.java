package site.maxing.tkextractor.compat;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 这是一个兼容层，用于安全地与 Mekanism API 交互。
 * 只有在 Mekanism 被加载时，才应该调用这个类中的方法。
 */
public class MekanismCompat {

    /**
     * 遍历 Mekanism 的 Chemical 注册表，提取所有自定义名称。
     * @return 一个包含 [翻译键, 显示名称] 的 Map
     */
    public static Map<String, String> extractChemicals() {
        Map<String, String> keyMap = new LinkedHashMap<>();

        // 遍历 Mekanism 的 Chemical 注册表
        for (Chemical chemical : MekanismAPI.CHEMICAL_REGISTRY) {
            String translationKey = chemical.getTranslationKey();
            Component displayNameComponent = chemical.getTextComponent(); // Chemical 直接提供了 Component

            if (translationKey == null || translationKey.isEmpty() || displayNameComponent == null) {
                continue;
            }

            String displayName = displayNameComponent.getString().trim();

            if (!displayName.isEmpty() && !displayName.equals(translationKey)) {
                keyMap.put(translationKey, displayName);
            }
        }
        return keyMap;
    }
}