package at.lowdfx.upgradeFurnace.util;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.bukkit.permissions.PermissionDefault.TRUE;

public final class Perms {
    public enum Perm {
        UPGRADE_FURNACE("upgradefurnace.upgrade.furnace", "/upgrade", TRUE);

        private final String permission;
        private final String commands;
        private final PermissionDefault def;

        Perm(String permission, String commands, PermissionDefault def) {
            this.permission = permission;
            this.commands = commands;
            this.def = def;
        }

        public String getPermission() {
            return permission;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    // Lädt die Berechtigungen aus der permissions.json und registriert sie.
    public static void loadPermissions() {
        Path permFile = UpgradeFurnace.PLUGIN_DIR.resolve("permissions.json");
        try {
            if (permFile.toFile().createNewFile()) {
                Map<String, Object> data = new LinkedHashMap<>();
                for (Perm perm : Perm.values()) {
                    Map<String, Object> permData = new LinkedHashMap<>();
                    permData.put("description", "Erlaubt die Benutzung von " + perm.commands);
                    permData.put("default", perm.def.name().toLowerCase(Locale.ROOT));
                    data.put(perm.permission, permData);
                }
                saveJson(data, permFile);
                UpgradeFurnace.LOG.info("Permission-Konfiguration erstellt.");
            }
        } catch (IOException e) {
            UpgradeFurnace.LOG.error("Konnte Permission-Datei nicht erstellen.");
        }

        PluginManager manager = Bukkit.getPluginManager();
        loadJson(permFile).forEach((s, o) -> {
            if (!(o instanceof Map<?, ?> map)) return;
            manager.addPermission(new Permission(s,
                    (String) map.get("description"),
                    PermissionDefault.valueOf(((String) map.get("default")).toUpperCase(Locale.ROOT))));
        });
    }

    private static void saveJson(Map<String, Object> data, Path file) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            UpgradeFurnace.LOG.error("Konnte JSON nicht speichern: " + e.getMessage());
        }
    }

    private static Map<String, Object> loadJson(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Object> result = GSON.fromJson(reader, MAP_TYPE);
            return result != null ? result : Map.of();
        } catch (IOException e) {
            return Map.of();
        }
    }

    public static boolean check(@NotNull Permissible source, @NotNull Perm perm) {
        return source.hasPermission(perm.permission);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean check(@NotNull CommandSourceStack source, @NotNull Perm perm) {
        return check(source.getSender(), perm);
    }
}
