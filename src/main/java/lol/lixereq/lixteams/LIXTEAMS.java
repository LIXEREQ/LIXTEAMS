package lol.lixereq.lixteams;

import lol.lixereq.lixteams.commands.commands;
import lol.lixereq.lixteams.commands.adminCommands;
import lol.lixereq.lixteams.data.datConfig;
import lol.lixereq.lixteams.teamUtils.teamUtils;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LIXTEAMS implements ModInitializer {
	public static final String MOD_ID = "lixteams";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void runDelayed(MinecraftServer server, Runnable task, int ticks) {
        if (ticks <= 0) {
            server.execute(task);
        } else {
            server.execute(() -> runDelayed(server, task, ticks - 1));
        }
    }

    @Override
	public void onInitialize() {
		LOGGER.info("Initialising LIXTEAMS Mod...");

        try {
            datConfig.InitialiseDatFolder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

		LOGGER.info("LIXTEAMS Mod Data Loaded!");

        commands.registerCommands();
        adminCommands.registerCommands();
        teamUtils.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server)
                -> runDelayed(server, () -> teamUtils.rebuildTeams(server), 3));

        LOGGER.info("Mod Successfully Initialized!");
    }
}