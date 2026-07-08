package com.guidumper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.InventoryType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Serializes whatever custom GUI the target player currently has open into
 * plugins/GuiDumper/dumps.json, so this server's own menus can be backed up
 * or rebuilt later without re-deriving them from the plugin's source.
 */
public class GuiDumpCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final GuiDumperPlugin plugin;

    public GuiDumpCommand(GuiDumperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or not online: " + args[0]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cConsole must specify a player: /guidump <player>");
            return true;
        }

        InventoryView view = target.getOpenInventory();
        Inventory top = view.getTopInventory();

        if (top.getType() == InventoryType.CRAFTING || top.getType() == InventoryType.CREATIVE) {
            sender.sendMessage("§c" + target.getName() + " does not have a custom GUI open right now.");
            return true;
        }

        JsonObject entry = new JsonObject();
        entry.addProperty("capturedAt", Instant.now().toString());
        entry.addProperty("capturedBy", sender.getName());
        entry.addProperty("player", target.getName());
        entry.addProperty("title", LEGACY.serialize(view.title()));
        entry.addProperty("inventoryType", top.getType().name());
        entry.addProperty("size", top.getSize());

        JsonArray slots = new JsonArray();
        ItemStack[] contents = top.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            slots.add(serializeItem(i, item));
        }
        entry.add("slots", slots);

        File file = new File(plugin.getDataFolder(), "dumps.json");
        JsonArray dumps = readExistingDumps(file);
        dumps.add(entry);

        if (!writeDumps(file, dumps)) {
            sender.sendMessage("§cFailed to write dumps.json - check the console for details.");
            return true;
        }

        sender.sendMessage("§aDumped \"" + entry.get("title").getAsString() + "§a\" ("
                + slots.size() + " item" + (slots.size() == 1 ? "" : "s") + ") to plugins/GuiDumper/dumps.json");
        return true;
    }

    private JsonObject serializeItem(int index, ItemStack item) {
        JsonObject json = new JsonObject();
        json.addProperty("index", index);
        json.addProperty("material", item.getType().getKey().toString());
        json.addProperty("amount", item.getAmount());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            if (meta.hasDisplayName()) {
                json.addProperty("displayName", LEGACY.serialize(meta.displayName()));
            }

            if (meta.hasLore()) {
                JsonArray lore = new JsonArray();
                for (Component line : meta.lore()) {
                    lore.add(LEGACY.serialize(line));
                }
                json.add("lore", lore);
            }

            if (meta.hasCustomModelData()) {
                json.addProperty("customModelData", meta.getCustomModelData());
            }

            if (meta.isUnbreakable()) {
                json.addProperty("unbreakable", true);
            }

            if (!meta.getEnchants().isEmpty()) {
                JsonObject enchants = new JsonObject();
                for (Map.Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
                    enchants.addProperty(e.getKey().getKey().toString(), e.getValue());
                }
                json.add("enchantments", enchants);
            }
        }

        return json;
    }

    private JsonArray readExistingDumps(File file) {
        if (!file.exists()) {
            return new JsonArray();
        }
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonArray existing = JsonParser.parseReader(reader).getAsJsonArray();
            return existing;
        } catch (IOException | IllegalStateException | com.google.gson.JsonSyntaxException e) {
            plugin.getLogger().warning("Could not read existing dumps.json, starting a new one: " + e.getMessage());
            return new JsonArray();
        }
    }

    private boolean writeDumps(File file, JsonArray dumps) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(dumps, writer);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write dumps.json: " + e.getMessage());
            return false;
        }
    }
}
