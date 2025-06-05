package fr.slaymi.loginmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.slaymi.loginmod.util.FileHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static fr.slaymi.loginmod.Loginmod.LOGIN_WORLD_KEY;

public class RegisterCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("register")
                    .then(CommandManager.argument("password", StringArgumentType.word())
                            .executes(RegisterCommand::execute)));
        });
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("This command can only be executed by players!"));
            return 0;
        }

        if (!player.getWorld().getRegistryKey().equals(LOGIN_WORLD_KEY)) {
            source.sendError(Text.literal("§cTu ne peux exécuter cette commande que dans le monde de connexion."));
            return 0;
        }

        String username = player.getGameProfile().getName();

        if (FileHandler.findPlayer(username) != null) {
            source.sendError(Text.literal("§cTu es déjà enregistré. Utilise §f/login §cpour te connecter."));
            return 0;
        }

        String password = StringArgumentType.getString(context, "password");
        FileHandler.addPlayer(username, password);

        player.sendMessage(Text.literal("§aTu es maintenant enregistré ! Utilise §f/login §apour te connecter."));

        return 1;
    }
}
