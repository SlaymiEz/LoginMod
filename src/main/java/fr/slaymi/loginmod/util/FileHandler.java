package fr.slaymi.loginmod.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final String PLAYERS_FILE = "loginmod_players.json";

    public static Path getPlayersFilePath() {
        return CONFIG_DIR.resolve(PLAYERS_FILE);
    }

    public static JsonArray readPlayersFile() {
        Path filePath = getPlayersFilePath();

        try {
            if (!Files.exists(filePath)) {
                JsonArray emptyArray = new JsonArray();
                writePlayersFile(emptyArray);
                return emptyArray;
            }

            String content = Files.readString(filePath);
            return GSON.fromJson(content, JsonArray.class);
        } catch (IOException e) {
            System.err.println("[LoginMod] Error reading players file: " + e.getMessage());
            return new JsonArray();
        }
    }

    public static void writePlayersFile(JsonArray playersArray) {
        Path filePath = getPlayersFilePath();

        try {
            Files.createDirectories(CONFIG_DIR);

            String json = GSON.toJson(playersArray);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            System.err.println("[LoginMod] Error writing players file: " + e.getMessage());
        }
    }

    public static void addPlayer(String username, String password) {
        JsonArray players = readPlayersFile();

        JsonObject newPlayer = new JsonObject();
        newPlayer.addProperty("username", username);
        newPlayer.addProperty("password", password);
        players.add(newPlayer);

        writePlayersFile(players);

        System.out.println("[LoginMod] Added player " + username + " to players file");
    }

    public static JsonObject findPlayer(String username) {
        JsonArray players = readPlayersFile();

        for (int i = 0; i < players.size(); i++) {
            JsonObject player = players.get(i).getAsJsonObject();
            if (player.get("username").getAsString().equals(username)) {
                return player;
            }
        }

        return null; // Player not found
    }

    public static void updatePlayerData(String username, JsonObject newData) {
        JsonArray players = readPlayersFile();
        boolean updated = false;

        for (int i = 0; i < players.size(); i++) {
            JsonObject player = players.get(i).getAsJsonObject();
            if (player.get("username").getAsString().equals(username)) {
                for (String key : newData.keySet()) {
                    player.add(key, newData.get(key));
                }
                updated = true;
                break;
            }
        }

        if (updated) {
            writePlayersFile(players);
            System.out.println("[LoginMod] Updated player " + username + " in players file");
        } else {
            System.err.println("[LoginMod] Player " + username + " not found for update");
        }

    }
}
