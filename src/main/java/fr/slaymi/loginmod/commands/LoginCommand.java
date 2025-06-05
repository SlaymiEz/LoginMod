package fr.slaymi.loginmod.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.slaymi.loginmod.Loginmod;
import fr.slaymi.loginmod.util.FileHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.Objects;

import static fr.slaymi.loginmod.Loginmod.LOGIN_WORLD_KEY;

public class LoginCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("login")
                .then(CommandManager.argument("password", StringArgumentType.word())
                        .executes(LoginCommand::execute))));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("This command can only be executed by players!"));
            return 0;
        }

        if (!player.getWorld().getRegistryKey().equals(LOGIN_WORLD_KEY)) {
            source.sendError(Text.literal("Tu ne peux exécuter cette commande que dans le monde de connexion."));
            return 0;
        }

        String username = player.getGameProfile().getName();
        String password = StringArgumentType.getString(context, "password");

        JsonObject playerData = FileHandler.findPlayer(username);
        if (playerData == null) {
            source.sendError(Text.literal("Tu n'es pas enregistré. Utilise §f/register §cpour t'enregistrer."));
            return 0;
        }

        String expectedPassword = playerData.get("password").getAsString();
        if (!password.equals(expectedPassword)) {
            source.sendError(Text.literal("Mot de passe incorrect. Si le problème persiste, contacte §fsami.tropbeau §csur Instagram."));
            return 0;
        }

        try {
            String worldId = playerData.has("world") ? playerData.get("world").getAsString() : "overworld";
            ServerWorld world = switch (worldId) {
                case "nether" -> Objects.requireNonNull(player.getServer()).getWorld(World.NETHER);
                case "end" -> Objects.requireNonNull(player.getServer()).getWorld(World.END);
                default -> Objects.requireNonNull(player.getServer()).getWorld(World.OVERWORLD);
            };

            float x = playerData.has("lastX") ? playerData.get("lastX").getAsFloat() : -63f;
            float y = playerData.has("lastY") ? playerData.get("lastY").getAsFloat() : 70f;
            float z = playerData.has("lastZ") ? playerData.get("lastZ").getAsFloat() : 27f;
            float yaw = playerData.has("lastYaw") ? playerData.get("lastYaw").getAsFloat() : 180f;
            float pitch = playerData.has("lastPitch") ? playerData.get("lastPitch").getAsFloat() : 0f;

            player.teleport(world, x, y, z, java.util.Set.of(), yaw, pitch, false);
            player.changeGameMode(GameMode.SURVIVAL);

            source.sendMessage(Text.literal("§aConnéxion réussie !"));
        } catch (Exception ex) {
            ex.printStackTrace();
            source.sendError(Text.literal("§cUne erreur est survenue. Réessaie ou contacte §fsami.tropbeau §csur Instagram."));
            return 0;
        }

        Loginmod.inLoginPlayers.remove(player);
        return 1;
    }
}
