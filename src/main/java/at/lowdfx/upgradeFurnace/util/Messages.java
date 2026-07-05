package at.lowdfx.upgradeFurnace.util;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;
import java.util.Map;

public final class Messages {
    private static final String FALLBACK_LANGUAGE = "en";
    private static YamlConfiguration fallbackMessages;
    private static YamlConfiguration messages;

    private Messages() {
    }

    public static void init(@NotNull UpgradeFurnace plugin) {
        saveLanguageFile(plugin, FALLBACK_LANGUAGE);

        String selectedLanguage = normalizeLanguage(Configuration.LANGUAGE);
        if (!FALLBACK_LANGUAGE.equals(selectedLanguage)) {
            saveLanguageFile(plugin, selectedLanguage);
        }

        fallbackMessages = loadLanguageFile(plugin, FALLBACK_LANGUAGE);
        messages = loadLanguageFile(plugin, selectedLanguage);

        if (messages == null) {
            UpgradeFurnace.LOG.warn("Language '{}' was not found. Falling back to '{}'.", selectedLanguage, FALLBACK_LANGUAGE);
            messages = fallbackMessages;
        }
    }

    public static Component component(@NotNull String key) {
        return Component.text(text(key));
    }

    public static Component component(@NotNull String key, @NotNull Map<String, String> replacements) {
        return Component.text(text(key, replacements));
    }

    public static String text(@NotNull String key) {
        return text(key, Map.of());
    }

    public static String text(@NotNull String key, @NotNull Map<String, String> replacements) {
        String value = null;
        if (messages != null) {
            value = messages.getString(key);
        }
        if (value == null && fallbackMessages != null) {
            value = fallbackMessages.getString(key);
        }
        if (value == null) {
            value = key;
        }

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return value;
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return FALLBACK_LANGUAGE;
        }
        return language.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    private static void saveLanguageFile(UpgradeFurnace plugin, String language) {
        String fileName = fileName(language);
        if (plugin.getResource(fileName) != null) {
            FileUpdater.updateYaml(plugin, fileName);
        }
    }

    private static YamlConfiguration loadLanguageFile(UpgradeFurnace plugin, String language) {
        File file = new File(plugin.getDataFolder(), fileName(language));
        if (!file.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private static String fileName(String language) {
        return "messages_" + language + ".yml";
    }
}
