package fr.slaymi.loginmod;

import com.google.gson.JsonObject;
import fr.slaymi.loginmod.commands.LoginCommand;
import fr.slaymi.loginmod.commands.RegisterCommand;
import fr.slaymi.loginmod.util.FileHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class Loginmod implements ModInitializer {

    public static final RegistryKey<World> LOGIN_WORLD_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("loginmod", "login_world")
    );

    public static final Set<ServerPlayerEntity> inLoginPlayers = new HashSet<>();

    @Override
    public void onInitialize() {
        System.out.println("[LoginMod] Initializing...");

        // Register commands
        LoginCommand.register();
        RegisterCommand.register();

        // Handle join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            handlePlayerLogin(player);
        });

        // Handle disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, sender) -> {
            ServerPlayerEntity player = handler.getPlayer();
            inLoginPlayers.remove(player);
            savePlayerData(player);
        });

        // Prevent movement in login world
        ServerTickEvents.END_SERVER_TICK.register(this::handleLoggedInPlayers);
    }

    private void handlePlayerLogin(ServerPlayerEntity player) {
        String username = player.getGameProfile().getName();
        player.sendMessage(net.minecraft.text.Text.literal("§eSalam Aleykoum " + username + "!"));

        teleportToLoginWorld(player);

        JsonObject playerData = FileHandler.findPlayer(username);
        if (playerData != null) {
            player.sendMessage(net.minecraft.text.Text.literal("§eUtilise §f/login §epour te connecter."));
        } else {
            player.sendMessage(net.minecraft.text.Text.literal("§eUtilise §f/register §epour t'enregistrer."));
        }
    }

    private void teleportToLoginWorld(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        ServerWorld loginWorld = server.getWorld(LOGIN_WORLD_KEY);
        if (loginWorld == null) {
            player.onDisconnect(); // Forcefully disconnect if the login world doesn't exist
            return;
        }

        server.execute(() -> {
            player.teleport(loginWorld, 0.0, 0.0, 0.0, Set.of(), 0.0F, 0.0F, false);
            player.changeGameMode(GameMode.SPECTATOR);
            inLoginPlayers.add(player);
            System.out.println("[LoginMod] Teleported " + player.getGameProfile().getName() + " to login world");
        });
    }

    private void handleLoggedInPlayers(MinecraftServer server) {
        // Prevent movement for players in the login world
        inLoginPlayers.removeIf(player -> !server.getPlayerManager().getPlayerList().contains(player)); // cleanup

        for (ServerPlayerEntity player : inLoginPlayers) {
            if (player.getWorld().getRegistryKey().equals(LOGIN_WORLD_KEY)) {
                player.setVelocity(0, 0, 0);
                player.teleport(server.getWorld(LOGIN_WORLD_KEY), 0, 0, 0, Set.of(), 0.0F, 0.0F, false);
            }
        }
    }

    private void savePlayerData(ServerPlayerEntity player) {
        String username = player.getGameProfile().getName();
        JsonObject playerData = FileHandler.findPlayer(username);

        if (playerData == null) {
            System.out.println("[LoginMod] Unknown player (likely disconnected from login world): " + username);
            return;
        }

        playerData.addProperty("lastX", player.getX());
        playerData.addProperty("lastY", player.getY());
        playerData.addProperty("lastZ", player.getZ());
        playerData.addProperty("lastYaw", player.getYaw());
        playerData.addProperty("lastPitch", player.getPitch());

        RegistryKey<World> key = player.getWorld().getRegistryKey();
        String worldName = switch (key.getValue().getPath()) {
            case "overworld" -> "overworld";
            case "the_nether" -> "nether";
            case "the_end" -> "end";
            default -> key.getValue().toString();
        };
        playerData.addProperty("world", worldName);

        FileHandler.updatePlayerData(username, playerData);
        System.out.println("[LoginMod] Saved data for " + username);
    }
}
