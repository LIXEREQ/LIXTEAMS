package lol.lixereq.lixteams.commands;

import lol.lixereq.lixteams.data.datManager;
import lol.lixereq.lixteams.teamUtils.teamUtils;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class adminCommands {

    private static final Map<UUID, Confirmation> pendingResets = new HashMap<>();

    public record Confirmation(String code, long expiryTime) {
    }

    private static String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("lixteamsAdmin")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.literal("memberCap")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) return 0;

                                            int newCap = IntegerArgumentType.getInteger(context, "value");
                                            CompoundTag data = datManager.get().getData();
                                            CompoundTag settings = data.getCompoundOrEmpty("settings");
                                            settings.putInt("maxMembers", newCap);

                                            try {
                                                datManager.get().save();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Team member cap set to " + newCap),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("maxTeamNameLength")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) return 0;

                                            int newLength = IntegerArgumentType.getInteger(context, "value");
                                            CompoundTag data = datManager.get().getData();
                                            CompoundTag settings = data.getCompoundOrEmpty("settings");
                                            settings.putInt("maxTeamNameLength", newLength);

                                            try {
                                                datManager.get().save();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Team name max length set to " + newLength),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("maxTeamTagLength")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) return 0;

                                            int newLength = IntegerArgumentType.getInteger(context, "value");
                                            CompoundTag data = datManager.get().getData();
                                            CompoundTag settings = data.getCompoundOrEmpty("settings");
                                            settings.putInt("maxTeamTagLength", newLength);

                                            try {
                                                datManager.get().save();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Team tag max length set to " + newLength),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("info")
                                .then(Commands.argument("teamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            datManager.get().getData().getCompoundOrEmpty("teams").keySet()
                                                    .forEach(team -> {
                                                        if (team.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                            builder.suggest(team);
                                                        }
                                                    });
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            MinecraftServer server = context.getSource().getServer();
                                            String teamName = StringArgumentType.getString(context, "teamName");

                                            Component info = datManager.get().getTeamInfo(server, teamName);

                                            context.getSource().sendSuccess(() -> info, false);
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("list")
                                .executes(context -> {
                                    CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");

                                    if (teams.isEmpty()) {
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("There are no teams on the server."),
                                                false
                                        );
                                        return 1;
                                    }

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Teams on the server:")
                                                    .withStyle(ChatFormatting.GOLD),
                                            false
                                    );

                                    for (String teamName : teams.keySet()) {
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("- ")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(teamName).withStyle(ChatFormatting.YELLOW)),
                                                false
                                        );
                                    }

                                    return 1;
                                })
                        )

                        .then(Commands.literal("reset")
                                .then(Commands.argument("code", StringArgumentType.string())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) return 0;

                                            UUID uuid = player.getUUID();
                                            String enteredCode = StringArgumentType.getString(context, "code");
                                            Confirmation confirm = pendingResets.get(uuid);

                                            if (confirm == null || System.currentTimeMillis() > confirm.expiryTime) {
                                                context.getSource().sendFailure(Component.literal("You haven't started a reset or the code has expired!"));
                                                pendingResets.remove(uuid);
                                                return 0;
                                            }

                                            if (!confirm.code.equalsIgnoreCase(enteredCode)) {
                                                context.getSource().sendFailure(Component.literal("Incorrect code!"));
                                                return 0;
                                            }

                                            MinecraftServer server = context.getSource().getServer();

                                            try {
                                                datManager.get().resetData(server);
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                            pendingResets.remove(uuid);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("All team data has been wiped!").withStyle(ChatFormatting.RED),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) return 0;

                                    UUID uuid = player.getUUID();
                                    if (pendingResets.containsKey(uuid)) {
                                        context.getSource().sendFailure(Component.literal(
                                                "You already have a pending reset! Enter your existing code or wait until it expires."
                                        ));
                                        return 0;
                                    }

                                    String code = generateCode();
                                    long expiry = System.currentTimeMillis() + 60_000;
                                    pendingResets.put(uuid, new Confirmation(code, expiry));

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("⚠ Are you sure you want to continue? This will wipe all data!").withStyle(ChatFormatting.RED),
                                            false
                                    );

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Please enter the code to confirm: /lixteamsAdmin reset " + code).withStyle(ChatFormatting.YELLOW),
                                            false
                                    );

                                    return 1;
                                })
                        )

                        .then(Commands.literal("blockSettings")
                                .then(Commands.argument("teamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            datManager.get().getData()
                                                    .getCompoundOrEmpty("teams")
                                                    .keySet()
                                                    .forEach(team -> {
                                                        if (team.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                            builder.suggest(team);
                                                        }
                                                    });
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    String teamName = StringArgumentType.getString(context, "teamName");
                                                    boolean value = BoolArgumentType.getBool(context, "value");

                                                    CompoundTag data = datManager.get().getData();
                                                    CompoundTag teams = data.getCompoundOrEmpty("teams");

                                                    if (!teams.contains(teamName)) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("Team '" + teamName + "' does not exist!")
                                                        );
                                                        return 0;
                                                    }

                                                    CompoundTag settings = data.getCompoundOrEmpty("settings");
                                                    ListTag blocked = settings.getListOrEmpty("blockTeamsSettings");

                                                    int index = -1;
                                                    for (int i = 0; i < blocked.size(); i++) {
                                                        if (teamName.equalsIgnoreCase(blocked.getString(i).orElse(""))) {
                                                            index = i;
                                                            break;
                                                        }
                                                    }

                                                    if (value) {
                                                        if (index != -1) {
                                                            context.getSource().sendFailure(
                                                                    Component.literal("This team is already blocked!")
                                                            );
                                                            return 0;
                                                        }

                                                        blocked.add(StringTag.valueOf(teamName));
                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal("Team '" + teamName + "' is now blocked from changing settings."),
                                                                false
                                                        );
                                                    } else {
                                                        if (index == -1) {
                                                            context.getSource().sendFailure(
                                                                    Component.literal("This team is not blocked already!")
                                                            );
                                                            return 0;
                                                        }

                                                        blocked.remove(index);
                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal("Team '" + teamName + "' can now change settings."),
                                                                false
                                                        );
                                                    }

                                                    settings.put("blockTeamsSettings", blocked);

                                                    try {
                                                        datManager.get().save();
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }

                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(Commands.literal("modifySettings")
                                .then(Commands.argument("teamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            datManager.get().getData().getCompoundOrEmpty("teams").keySet()
                                                    .forEach(team -> {
                                                        if (team.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                            builder.suggest(team);
                                                        }
                                                    });
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String teamName = StringArgumentType.getString(context, "teamName");
                                            ServerPlayer player = context.getSource().getPlayer();
                                            assert player != null;

                                            try {
                                                datManager.get().handleSettingsAdmin(player.createCommandSourceStack(), teamName, null, null);
                                            } catch (IOException | CommandSyntaxException e) {
                                                throw new RuntimeException(e);
                                            }

                                            return 1;
                                        })
                                                .then(Commands.argument("setting", StringArgumentType.string())
                                                        .suggests((context, builder) -> {
                                                            String teamName = StringArgumentType.getString(context, "teamName");
                                                            CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");
                                                            CompoundTag teamData = teams.getCompoundOrEmpty(teamName);
                                                            String prefix = builder.getRemaining();

                                                            CompoundTag settings = teamData.getCompoundOrEmpty("settings");
                                                            for (String key : settings.keySet()) {
                                                                if (key.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                                    builder.suggest(key);
                                                                }
                                                            }
                                                            if ("currencyDelay".toLowerCase().startsWith(prefix.toLowerCase())) {
                                                                builder.suggest("currencyDelay");
                                                            }

                                                            return builder.buildFuture();
                                                        })
                                                        .executes(context -> {
                                                            String teamName = StringArgumentType.getString(context, "teamName");
                                                            String setting = StringArgumentType.getString(context, "setting");
                                                            ServerPlayer player = context.getSource().getPlayer();
                                                            assert player != null;

                                                            if ("currencyDelay".equals(setting)) {
                                                                long delay = datManager.get().getTeamDelaySeconds(teamName);
                                                                context.getSource().sendSuccess(
                                                                        () -> Component.literal("Current currency delay for '" + teamName + "': " + delay + " seconds"),
                                                                        false
                                                                );
                                                                return 1;
                                                            }

                                                            try {
                                                                datManager.get().handleSettingsAdmin(player.createCommandSourceStack(), teamName, setting, null);
                                                            } catch (IOException | CommandSyntaxException e) {
                                                                throw new RuntimeException(e);
                                                            }

                                                            return 1;
                                                        })
                                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                                .executes(context -> {
                                                                    String teamName = StringArgumentType.getString(context, "teamName");
                                                                    String setting = StringArgumentType.getString(context, "setting");
                                                                    int value = IntegerArgumentType.getInteger(context, "value");
                                                                    ServerPlayer player = context.getSource().getPlayer();
                                                                    assert player != null;

                                                                    if ("currencyDelay".equals(setting)) {
                                                                        long delaySeconds = value * 60L;
                                                                        try {
                                                                            datManager.get().setTeamDelaySeconds(teamName, delaySeconds);
                                                                        } catch (IOException e) {
                                                                            context.getSource().sendFailure(Component.literal("Failed to save currency delay."));
                                                                            e.printStackTrace();
                                                                            return 0;
                                                                        }

                                                                        context.getSource().sendSuccess(
                                                                                () -> Component.literal("Currency delay for team '" + teamName + "' set to " + value + " minute(s) (" + delaySeconds + " seconds)."),
                                                                                false
                                                                        );
                                                                        return 1;
                                                                    }

                                                                    try {
                                                                        datManager.get().handleSettingsAdmin(player.createCommandSourceStack(), teamName, setting, value != 0);
                                                                    } catch (IOException | CommandSyntaxException e) {
                                                                        throw new RuntimeException(e);
                                                                    }

                                                                    teamUtils.rebuildTeams(context.getSource().getServer());

                                                                    context.getSource().sendSuccess(
                                                                            () -> Component.literal("Admin set '" + setting + "' for team '" + teamName + "' to " + (value != 0)),
                                                                            false
                                                                    );
                                                                    return 1;
                                                                })
                                                        )
                                        )
                                )
                        )

                        .then(Commands.literal("addCurrency")
                                .then(Commands.argument("playerName", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                                    .filter(player -> player.getGameProfile().name().toLowerCase().startsWith(prefix.toLowerCase()))
                                                    .forEach(player -> builder.suggest(player.getGameProfile().name()));
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("currencyName", StringArgumentType.string())
                                                .suggests((context, builder) -> {
                                                    String prefix = builder.getRemaining();
                                                    datManager.get().getAllCurrencyNames()
                                                            .forEach(currency -> {
                                                                if (currency.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                                    builder.suggest(currency);
                                                                }
                                                            });
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(context -> {
                                                            ServerPlayer player = context.getSource().getPlayer();
                                                            assert player != null;

                                                            String targetName = StringArgumentType.getString(context, "playerName");
                                                            ServerPlayer targetPlayer = context.getSource().getServer()
                                                                    .getPlayerList()
                                                                    .getPlayerByName(targetName);

                                                            if (targetPlayer == null) {
                                                                context.getSource().sendFailure(Component.literal("Player not found or not online!"));
                                                                return 0;
                                                            }

                                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                                            String currencyName = StringArgumentType.getString(context, "currencyName");

                                                            try {
                                                                datManager.get().addPlayerCurrencyBalance(targetPlayer.getUUID().toString(), currencyName, amount);
                                                            } catch (IOException e) {
                                                                context.getSource().sendFailure(Component.literal("Failed to add currency."));
                                                                e.printStackTrace();
                                                                return 0;
                                                            }

                                                            String currencyTagStr = datManager.get().getTeamCurrencyTagStringForCurrency(currencyName);
                                                            String targetNameStr = targetPlayer.getGameProfile().name();
                                                            Component addedMsg = Component.literal("Added ")
                                                                    .withStyle(ChatFormatting.YELLOW)
                                                                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.YELLOW))
                                                                    .append(Component.literal(" "))
                                                                    .append(Component.literal(currencyName).withStyle(ChatFormatting.YELLOW))
                                                                    .append(Component.literal(" ["))
                                                                    .append(Component.literal(currencyTagStr).withStyle(ChatFormatting.GOLD))
                                                                    .append(Component.literal("]"))
                                                                    .append(Component.literal(" to "))
                                                                    .append(Component.literal(targetNameStr).withStyle(ChatFormatting.GREEN))
                                                                    .append(Component.literal("!"));

                                                            context.getSource().sendSuccess(() -> addedMsg, false);
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )

                        .then(Commands.literal("activatePendingCurrency")
                                .then(Commands.argument("playerName", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                                    .filter(p -> p.getGameProfile().name().toLowerCase().startsWith(prefix.toLowerCase()))
                                                    .forEach(p -> builder.suggest(p.getGameProfile().name()));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            assert player != null;

                                            String targetName = StringArgumentType.getString(context, "playerName");
                                            ServerPlayer targetPlayer = context.getSource().getServer()
                                                    .getPlayerList()
                                                    .getPlayerByName(targetName);

                                            if (targetPlayer == null) {
                                                context.getSource().sendFailure(Component.literal("Player not found or not online!"));
                                                return 0;
                                            }

                                            String teamName = datManager.get().getTeam(targetPlayer.getUUID());
                                            if (teamName == null) {
                                                context.getSource().sendFailure(Component.literal("This player is not in a team!"));
                                                return 0;
                                            }

                                            if (!datManager.get().hasPendingCurrency(teamName, targetPlayer.getUUID().toString())) {
                                                context.getSource().sendFailure(Component.literal("This player has no pending currency!"));
                                                return 0;
                                            }

                                            String amount = String.valueOf(datManager.get().getPendingAmount(teamName, targetPlayer.getUUID().toString()));
                                            String currencyName = datManager.get().getTeamCurrencyName(teamName);

                                            try {
                                                datManager.get().activatePendingCurrency(teamName, targetPlayer.getUUID().toString());
                                            } catch (IOException e) {
                                                context.getSource().sendFailure(Component.literal("Failed to activate pending currency."));
                                                e.printStackTrace();
                                                return 0;
                                            }

                                            String currencyTagStr = datManager.get().getTeamCurrencyTagString(teamName);
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Activated pending currency: " + amount + " " + currencyName + " [" + currencyTagStr + "] for " + targetName),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("cancelPendingCurrency")
                                .then(Commands.argument("playerName", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                                    .filter(p -> p.getGameProfile().name().toLowerCase().startsWith(prefix.toLowerCase()))
                                                    .forEach(p -> builder.suggest(p.getGameProfile().name()));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            assert player != null;

                                            String targetName = StringArgumentType.getString(context, "playerName");
                                            ServerPlayer targetPlayer = context.getSource().getServer()
                                                    .getPlayerList()
                                                    .getPlayerByName(targetName);

                                            if (targetPlayer == null) {
                                                context.getSource().sendFailure(Component.literal("Player not found or not online!"));
                                                return 0;
                                            }

                                            String teamName = datManager.get().getTeam(targetPlayer.getUUID());
                                            if (teamName == null) {
                                                context.getSource().sendFailure(Component.literal("This player is not in a team!"));
                                                return 0;
                                            }

                                            if (!datManager.get().hasPendingCurrency(teamName, targetPlayer.getUUID().toString())) {
                                                context.getSource().sendFailure(Component.literal("This player has no pending currency!"));
                                                return 0;
                                            }

                                            try {
                                                datManager.get().cancelPendingCurrency(teamName, targetPlayer.getUUID().toString());
                                            } catch (IOException e) {
                                                context.getSource().sendFailure(Component.literal("Failed to cancel pending currency."));
                                                e.printStackTrace();
                                                return 0;
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Cancelled pending currency for " + targetName),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("removePlayerCurrency")
                                .then(Commands.argument("playerName", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                                    .filter(player -> player.getGameProfile().name().toLowerCase().startsWith(prefix.toLowerCase()))
                                                    .forEach(player -> builder.suggest(player.getGameProfile().name()));
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayer();
                                                    assert player != null;

                                                    String targetName = StringArgumentType.getString(context, "playerName");
                                                    ServerPlayer targetPlayer = context.getSource().getServer()
                                                            .getPlayerList()
                                                            .getPlayerByName(targetName);

                                                    if (targetPlayer == null) {
                                                        context.getSource().sendFailure(Component.literal("Player not found or not online!"));
                                                        return 0;
                                                    }

                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    String teamName = datManager.get().getTeam(targetPlayer.getUUID());

                                                    if (teamName == null) {
                                                        context.getSource().sendFailure(Component.literal("This player is not in a team!"));
                                                        return 0;
                                                    }

                                                    String currencyName = datManager.get().getTeamCurrencyName(teamName);
                                                    if (currencyName.isEmpty()) {
                                                        context.getSource().sendFailure(Component.literal("This team does not have a currency!"));
                                                        return 0;
                                                    }

                                                    try {
                                                        datManager.get().subtractPlayerCurrencyBalance(targetPlayer.getUUID().toString(), currencyName, amount);
                                                    } catch (IOException e) {
                                                        context.getSource().sendFailure(Component.literal("Failed to remove currency."));
                                                        e.printStackTrace();
                                                        return 0;
                                                    }

                                                    String currencyTagStr2 = datManager.get().getTeamCurrencyTagString(teamName);
                                                    String targetNameStr2 = targetPlayer.getGameProfile().name();
                                                    Component removedMsg = Component.literal("Removed ")
                                                            .withStyle(ChatFormatting.YELLOW)
                                                            .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.YELLOW))
                                                            .append(Component.literal(" "))
                                                            .append(Component.literal(currencyName).withStyle(ChatFormatting.YELLOW))
                                                            .append(Component.literal(" ["))
                                                            .append(Component.literal(currencyTagStr2).withStyle(ChatFormatting.GOLD))
                                                            .append(Component.literal("]"))
                                                            .append(Component.literal(" from "))
                                                            .append(Component.literal(targetNameStr2).withStyle(ChatFormatting.RED))
                                                            .append(Component.literal("!"));

                                                    context.getSource().sendSuccess(() -> removedMsg, false);
                                                    return 1;
                                                })
                                        )
                                )
                        )


        ));
    }
}
