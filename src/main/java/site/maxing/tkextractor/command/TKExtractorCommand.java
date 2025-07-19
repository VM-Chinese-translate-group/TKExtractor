package site.maxing.tkextractor.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import site.maxing.tkextractor.TKExtractor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TKExtractorCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tkextractor")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("namespace", StringArgumentType.string())
                                .suggests(TKExtractorCommand::suggestNamespaces)
                                .executes(TKExtractorCommand::execute)
                        )
        );
    }

    // 这个建议现在是提供所有可能的命名空间，而不仅仅是modid
    private static CompletableFuture<Suggestions> suggestNamespaces(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        // 合并真实的 Mod ID 和所有在物品注册表中出现的命名空间，去重
        Stream<String> modIds = ModList.get().getMods().stream().map(IModInfo::getModId);
        Stream<String> itemNamespaces = BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::getNamespace);

        Stream<String> allNamespaces = Stream.concat(modIds, itemNamespaces).distinct().sorted();
        Stream<String> suggestions = Stream.concat(Stream.of("_all_"), allNamespaces);

        return SharedSuggestionProvider.suggest(suggestions, builder);
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String namespaceArg = StringArgumentType.getString(context, "namespace");
        CommandSourceStack source = context.getSource();

        source.sendSystemMessage(Component.translatable("commands.tkextractor.scanning"));

        // 1. 扫描并收集所有自定义名称
        Map<String, String> allCustomNames = new LinkedHashMap<>();
        allCustomNames.putAll(extractAllFromRegistry(BuiltInRegistries.ITEM));
        allCustomNames.putAll(extractAllFromRegistry(BuiltInRegistries.BLOCK));
        allCustomNames.putAll(extractAllFromRegistry(BuiltInRegistries.FLUID));
        allCustomNames.putAll(extractAllFromRegistry(BuiltInRegistries.ENTITY_TYPE));

        source.sendSystemMessage(Component.translatable("commands.tkextractor.scan.complete", allCustomNames.size()));

        // 2. 根据参数进行过滤
        Map<String, String> finalData;
        String outputFileName;

        if (namespaceArg.equals("_all_")) {
            finalData = allCustomNames;
            outputFileName = "ALL_NAMESPACES";
            source.sendSystemMessage(Component.translatable("commands.tkextractor.filter.all"));
        } else {
            source.sendSystemMessage(Component.translatable("commands.tkextractor.filter.namespace", namespaceArg));
            finalData = allCustomNames.entrySet().stream()
                    .filter(entry -> getNamespaceFromKey(entry.getKey()).equals(namespaceArg))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));
            outputFileName = namespaceArg;
        }

        // 3. 保存到文件
        saveToFile(source, outputFileName, finalData);

        return 1;
    }

    /**
     * 遍历整个注册表，提取所有具有自定义显示名称的条目。
     * @param registry 要扫描的注册表
     * @return 一个包含 [翻译键, 显示名称] 的 Map
     */
    private static <T> Map<String, String> extractAllFromRegistry(Registry<T> registry) {
        Map<String, String> keyMap = new LinkedHashMap<>();
        for (T value : registry) {
            String translationKey = getTranslationKey(value);
            String rawDisplayName = getDisplayName(value);

            if (translationKey.isEmpty() || rawDisplayName.isEmpty()) {
                continue;
            }

            String displayName = rawDisplayName.trim(); // 1. 去除首尾空格
            if (displayName.startsWith("[") && displayName.endsWith("]")) {
                // 2. 如果被方括号包裹，则去除
                displayName = displayName.substring(1, displayName.length() - 1).trim(); // 再次trim以防括号内有空格
            }

            if (!displayName.isEmpty() && !displayName.equals(translationKey)) {
                keyMap.put(translationKey, displayName);
            }
        }
        return keyMap;
    }

    // 这些辅助方法现在变得更通用
    private static <T> String getTranslationKey(T registryObject) {
        if (registryObject instanceof Item item) return item.getDescriptionId();
        if (registryObject instanceof Block block) return block.getDescriptionId();
        if (registryObject instanceof Fluid fluid) return fluid.getFluidType().getDescriptionId();
        if (registryObject instanceof EntityType<?> entityType) return entityType.getDescriptionId();
        return "";
    }

    private static <T> String getDisplayName(T registryObject) {
        try {
            if (registryObject instanceof Item item) {
                return new ItemStack(item).getDisplayName().getString();
            } else if (registryObject instanceof Block block) {
                // --- 核心修正：处理没有物品的方块 ---
                if (block.asItem() == Items.AIR) {
                    // 如果方块没有物品形式（例如流体源），则直接获取方块的名称
                    return block.getName().getString();
                } else {
                    // 否则，通过其物品形式获取名称
                    return new ItemStack(block).getDisplayName().getString();
                }
                // --- 修正结束 ---
            } else if (registryObject instanceof Fluid fluid) {
                return fluid.getFluidType().getDescription().getString();
            } else if (registryObject instanceof EntityType<?> entityType) {
                return entityType.getDescription().getString();
            }
        } catch (Exception e) { /* 忽略错误 */ }
        return "";
    }

    private static String getNamespaceFromKey(String key) {
        String[] parts = key.split("\\.");
        if (parts.length > 1) {
            return parts[1];
        }
        return "";
    }

    private static void saveToFile(CommandSourceStack source, String fileName, Map<String, String> data) {
        if (data.isEmpty()) {
            source.sendSystemMessage(Component.translatable("commands.tkextractor.error.no_keys", fileName));
            return;
        }

        // 写入文件
        try {
            File outputDir = new File("tkextractor_output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File outputFile = new File(outputDir, fileName + ".json");

            try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }

            source.sendSystemMessage(Component.translatable("commands.tkextractor.success", data.size(), fileName));
            String clickPath; // 用于点击事件的路径
            String displayPath; // 用于在聊天中显示的路径

            try {
                // 优先尝试获取规范路径 (Resolves '..' and '.')
                clickPath = outputDir.getCanonicalPath();
                displayPath = outputFile.getCanonicalPath();
            } catch (IOException e) {
                // 如果失败，则回退到绝对路径，这不会抛出异常
                TKExtractor.LOGGER.warn("Could not resolve canonical path, falling back to absolute path. This is usually safe.", e);
                clickPath = outputDir.getAbsolutePath();
                displayPath = outputFile.getAbsolutePath();
            }

            // 创建一个可点击的组件，指向输出文件夹
            final String finalClickPath = clickPath;
            Component filePathComponent = Component.literal(displayPath)
                    .withStyle(style -> style
                            .withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, finalClickPath)));

            // 发送带有可点击链接的消息
            source.sendSystemMessage(Component.translatable("commands.tkextractor.file_saved_to", filePathComponent));

        } catch (IOException e) {
            source.sendFailure(Component.translatable("commands.tkextractor.error.write_failed", e.getMessage()));
            e.printStackTrace();
        }
    }
}