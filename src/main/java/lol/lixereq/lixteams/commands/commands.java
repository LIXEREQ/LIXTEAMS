package lol.lixereq.lixteams.commands;

import lol.lixereq.lixteams.data.datManager;
import lol.lixereq.lixteams.teamUtils.teamChatManager;
import lol.lixereq.lixteams.teamUtils.teamUtils;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("ALL")
public class commands {
    public static final String MOD_ID = "lixteams";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("lixteams")

                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("tag", StringArgumentType.string()).executes(context -> {
                                                    String teamName = StringArgumentType.getString(context, "name");
                                                    String teamTag = StringArgumentType.getString(context, "tag");
                                                     ServerPlayer player = context.getSource().getPlayer();
                                                     if (player == null) {
                                                         context.getSource().sendFailure(Component.literal("Player not found!"));
                                                         return 0;
                                                     }
                                                     UUID ownerUuid = player.getUUID();

                                                    CommandSourceStack source = context.getSource();
                                                    MinecraftServer server = source.getServer();

                                                    datManager.get().addTeam(teamName, teamTag, ownerUuid);

                                                    teamUtils.rebuildTeams(server);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.nullToEmpty("Successfully created team " + teamName),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )

                                        .then(Commands.literal("disband")
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayer();
                                                    if (player == null) {
                                                        context.getSource().sendFailure(Component.literal("Player not found!"));
                                                        return 0;
                                                    }
                                                    UUID ownerUuid = player.getUUID();
                                                    try {
                                                        datManager.get().removeTeam(ownerUuid);
                                                    } catch (IOException e) {
                                                        context.getSource().sendFailure(Component.literal("Failed to disband team: " + e.getMessage()));
                                                        return 0;
                                                    }

                                    CommandSourceStack source = context.getSource();
                                    MinecraftServer server = source.getServer();

                                    teamUtils.rebuildTeams(server);

                                    context.getSource().sendSuccess(
                                            () -> Component.nullToEmpty("Successfully Disbanded team"),
                                            false
                                    );
                                    return 1;
                                })
                        )

                        .then(Commands.literal("leave")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    assert player != null;
                                    UUID ownerUuid = player.getUUID();
                                    try {
                                        datManager.get().leaveTeam(ownerUuid);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }

                                    CommandSourceStack source = context.getSource();
                                    MinecraftServer server = source.getServer();

                                    teamUtils.rebuildTeams(server);

                                    context.getSource().sendSuccess(
                                            () -> Component.nullToEmpty("Successfully left team"),
                                            false
                                    );
                                    return 1;
                                })
                        )

                        .then(Commands.literal("tm")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    UUID uuid = player.getUUID();

                                    if (!datManager.get().isInTeam(uuid)) {
                                        context.getSource().sendFailure(Component.literal("You are not in a team!"));
                                        return 0;
                                    }

                                    boolean enabled = teamChatManager.toggle(uuid);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal(enabled ? "Team Chat Enabled" : "Team Chat Disabled"),
                                            false
                                    );
                                    return 1;
                                })
                        )

                        .then(Commands.literal("join")
                                .then(Commands.argument("name", StringArgumentType.string())
                                    .executes(context -> {
                                        String teamName = StringArgumentType.getString(context, "name");
                                        ServerPlayer player = context.getSource().getPlayer();
                                        assert player != null;
                                        UUID ownerUuid = player.getUUID();

                                        CommandSourceStack source = context.getSource();
                                        MinecraftServer server = source.getServer();

                                        datManager.get().sendRequest(teamName, ownerUuid, server);

                                        context.getSource().sendSuccess(
                                                () -> Component.nullToEmpty("Sent Request to " + teamName),
                                                false
                                        );
                                        return 1;
                                    })
                                )
                        )

                        .then(Commands.literal("accept")
                                .then(Commands.argument("playerName", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                                    .filter(player -> player.getGameProfile().name().toLowerCase().startsWith(prefix.toLowerCase()))
                                                    .forEach(player -> builder.suggest(player.getGameProfile().name()));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer owner = context.getSource().getPlayer();
                                            assert owner != null;
                                            UUID ownerUUID = owner.getUUID();

                                            String targetName = StringArgumentType.getString(context, "playerName");
                                            UUID targetUUID;

                                            try {
                                                targetUUID = UUID.fromString(targetName);
                                            } catch (IllegalArgumentException e) {
                                                ServerPlayer targetPlayer = context.getSource().getServer()
                                                        .getPlayerList()
                                                        .getPlayerByName(targetName);

                                                if (targetPlayer == null) {
                                                    context.getSource().sendFailure(Component.literal("Player not found or not online!"));
                                                    return 0;
                                                }

                                                targetUUID = targetPlayer.getUUID();
                                            }

                                            try {
                                                datManager.get().handleRequest(ownerUUID, targetUUID, true);
                                            } catch (CommandSyntaxException e) {
                                                context.getSource().sendFailure((Component) e.getRawMessage());
                                                return 0;
                                            } catch (IOException e) {
                                                context.getSource().sendFailure(Component.literal("An internal error occurred while saving the team data."));
                                                e.printStackTrace();
                                                return 0;
                                            }

                                            CommandSourceStack source = context.getSource();
                                            MinecraftServer server = source.getServer();
                                            teamUtils.rebuildTeams(server);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Accepted join request from " + targetName),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("deny")
                                .then(Commands.argument("playerName", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                                    .filter(player -> player.getGameProfile().name().toLowerCase().startsWith(prefix.toLowerCase()))
                                                    .forEach(player -> builder.suggest(player.getGameProfile().name()));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer owner = context.getSource().getPlayer();
                                            assert owner != null;
                                            UUID ownerUUID = owner.getUUID();

                                            String targetName = StringArgumentType.getString(context, "playerName");
                                            ServerPlayer targetPlayer = context.getSource().getServer()
                                                    .getPlayerList().getPlayerByName(targetName);

                                            if (targetPlayer == null) {
                                                context.getSource().sendFailure(Component.literal("Player not found or not online!"));
                                                return 0;
                                            }

                                            UUID targetUUID = targetPlayer.getUUID();

                                            try {
                                                datManager.get().handleRequest(ownerUUID, targetUUID, false);
                                            } catch (IOException | CommandSyntaxException e) {
                                                throw new RuntimeException(e);
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Denied join request from " + targetName),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("invite")
                                .then(Commands.argument("playerName", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayer owner = context.getSource().getPlayer();
                                            ServerPlayer target = context.getSource()
                                                    .getServer()
                                                    .getPlayerList()
                                                    .getPlayerByName(StringArgumentType.getString(context, "playerName"));

                                            if (target == null) {
                                                context.getSource().sendFailure(Component.literal("Player not online!"));
                                                return 0;
                                            }

                                            try {
                                                datManager.get().sendInvite(
                                                        owner.getUUID(),
                                                        target.getUUID(),
                                                        context.getSource().getServer()
                                                );
                                            } catch (CommandSyntaxException e) {
                                                context.getSource().sendFailure((Component) e.getRawMessage());
                                                return 0;
                                            } catch (Exception e) {
                                                context.getSource().sendFailure(Component.literal("Internal error occured, please contact server owner."));
                                                e.printStackTrace();
                                                return 0;
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Invite sent."),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("invAccept")
                                .then(Commands.argument("teamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            String prefix = builder.getRemaining();
                                            if (player != null) {
                                                datManager.get()
                                                        .getInvitedTeams(player.getUUID())
                                                        .forEach(team -> {
                                                            if (team.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                                builder.suggest(team);
                                                            }
                                                        });
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) return 0;

                                            String teamName = StringArgumentType.getString(context, "teamName");

                                            try {
                                                datManager.get().handleInvite(
                                                        player.getUUID(),
                                                        teamName,
                                                        true
                                                );
                                            } catch (CommandSyntaxException e) {
                                                context.getSource().sendFailure((Component) e.getRawMessage());
                                                return 0;
                                            } catch (IOException e) {
                                                context.getSource().sendFailure(Component.literal("Failed to save team data."));
                                                e.printStackTrace();
                                                return 0;
                                            }

                                            teamUtils.rebuildTeams(context.getSource().getServer());

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Joined team ")
                                                            .append(Component.literal(teamName).withStyle(ChatFormatting.YELLOW)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("invDeny")
                                .then(Commands.argument("teamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            String prefix = builder.getRemaining();
                                            if (player != null) {
                                                datManager.get()
                                                        .getInvitedTeams(player.getUUID())
                                                        .forEach(team -> {
                                                            if (team.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                                builder.suggest(team);
                                                            }
                                                        });
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) return 0;

                                            String teamName = StringArgumentType.getString(context, "teamName");

                                            try {
                                                datManager.get().handleInvite(
                                                        player.getUUID(),
                                                        teamName,
                                                        false
                                                );
                                            } catch (CommandSyntaxException e) {
                                                context.getSource().sendFailure((Component) e.getRawMessage());
                                                return 0;
                                            } catch (IOException e) {
                                                context.getSource().sendFailure(Component.literal("Failed to save team data."));
                                                e.printStackTrace();
                                                return 0;
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Denied invite from ")
                                                            .append(Component.literal(teamName).withStyle(ChatFormatting.YELLOW)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("info")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) return 0;

                                    String playerUuid = player.getUUID().toString();
                                    CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");

                                    CompoundTag playerTeam = null;
                                    String teamName = null;

                                    for (String key : teams.keySet()) {
                                        CompoundTag team = teams.getCompoundOrEmpty(key);

                                        if (team.getString("owner").orElse("").equals(playerUuid)) {
                                            playerTeam = team;
                                            teamName = key;
                                            break;
                                        }

                                        var members = team.getListOrEmpty("members");
                                        for (int i = 0; i < members.size(); i++) {
                                            if (members.getString(i).orElse("").equals(playerUuid)) {
                                                playerTeam = team;
                                                teamName = key;
                                                break;
                                            }
                                        }

                                        if (playerTeam != null) break;
                                    }

                                    if (playerTeam == null) {
                                        context.getSource().sendSuccess(() -> Component.literal("You are not in a team!"), false);
                                        return 0;
                                    }

                                    String teamTag = playerTeam.getString("teamTag").orElse(teamName);
                                    String ownerUuid = playerTeam.getString("owner").orElse("");
                                    String ownerName = "Unknown";

                                    ServerPlayer owner = null;
                                    try {
                                        if (player.level() instanceof ServerLevel serverWorld) {
                                            MinecraftServer server = serverWorld.getServer();
                                            owner = server.getPlayerList().getPlayer(UUID.fromString(ownerUuid));
                                        }
                                        if (owner != null) ownerName = String.valueOf(owner.asLivingEntity().getName().getString());
                                    } catch (IllegalArgumentException ignored) {}

                                    var membersList = playerTeam.getListOrEmpty("members");
                                    StringBuilder membersText = new StringBuilder();
                                    AtomicInteger offlineCount = new AtomicInteger();

                                    for (int i = 0; i < membersList.size(); i++) {
                                        membersList.getString(i).ifPresent(uuidStr -> {
                                            try {
                                                ServerPlayer member = null;
                                                if (player.level() instanceof ServerLevel serverWorld) {
                                                    MinecraftServer server = serverWorld.getServer();
                                                    member = server.getPlayerList().getPlayer(UUID.fromString(uuidStr));
                                                }
                                                if (member != null) {
                                                    if (membersText.length() > 0) membersText.append(", ");
                                                    membersText.append(member.asLivingEntity().getName().getString());
                                                } else {
                                                    offlineCount.getAndIncrement();
                                                }
                                            } catch (IllegalArgumentException ignored) {
                                                offlineCount.getAndIncrement();
                                            }
                                        });
                                    }

                                    if (offlineCount.get() > 0) {
                                        if (membersText.length() > 0) membersText.append(", ");
                                        membersText.append("(").append(offlineCount.get()).append(") Offline");
                                    }

                                    String currencyName = playerTeam.getString("currencyName").orElse("");
                                    ListTag currencyTag = playerTeam.getListOrEmpty("currencyTag");
                                    StringBuilder currencyTagBuilder = new StringBuilder();
                                    for (int i = 0; i < currencyTag.size(); i++) {
                                        currencyTagBuilder.append(currencyTag.getString(i).orElse(""));
                                    }
                                    boolean hasCurrency = !currencyName.isEmpty() || currencyTag.size() > 0;

                                    final MutableComponent[] infoMessage = new MutableComponent[1];
                                    if (hasCurrency) {
                                        infoMessage[0] = Component.literal("Team Info\n")
                                                .append(Component.literal("Team Name: " + teamName + "\n"))
                                                .append(Component.literal("Team Tag: " + teamTag + "\n"))
                                                .append(Component.literal("Owner: " + ownerName + "\n"))
                                                .append(Component.literal("Members: " + (membersText.length() > 0 ? membersText : "None")))
                                                .append(Component.literal("\nCurrency: "))
                                                .append(Component.literal(currencyName).withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(" ["))
                                                .append(Component.literal(currencyTagBuilder.toString()).withStyle(ChatFormatting.GOLD))
                                                .append(Component.literal("]"));
                                    } else {
                                        infoMessage[0] = Component.literal("Team Info\n")
                                                .append(Component.literal("Team Name: " + teamName + "\n"))
                                                .append(Component.literal("Team Tag: " + teamTag + "\n"))
                                                .append(Component.literal("Owner: " + ownerName + "\n"))
                                                .append(Component.literal("Members: " + (membersText.length() > 0 ? membersText : "None")));
                                    }

                                    context.getSource().sendSuccess(() -> infoMessage[0], false);

                                    return 1;
                                })
                        )

                        .then(Commands.literal("settings")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    assert player != null;

                                    String teamName = datManager.get().getTeam(player.getUUID());
                                    if (teamName != null) {
                                        ListTag blocked = datManager.get()
                                                .getData()
                                                .getCompoundOrEmpty("settings")
                                                .getListOrEmpty("blockTeamsSettings");

                                        for (int i = 0; i < blocked.size(); i++) {
                                            if (teamName.equalsIgnoreCase(blocked.getString(i).orElse(""))) {
                                                context.getSource().sendFailure(
                                                        Component.literal(
                                                                "Server Admin has disabled you from changing your team settings, please contact the Server's Admin!"
                                                        )
                                                );
                                                return 0;
                                            }
                                        }
                                    }

                                    try {
                                        assert player != null;
                                        datManager.get().handleSettings(player, null, null);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("setting", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");
                                            String prefix = builder.getRemaining();

                                            CompoundTag teamData = null;
                                            for (String teamName : teams.keySet()) {
                                                CompoundTag team = teams.getCompoundOrEmpty(teamName);
                                                assert player != null;
                                                if (team.getString("owner").orElse("").equals(player.getUUID().toString())) {
                                                    teamData = team;
                                                    break;
                                                }
                                            }

                                            if (teamData == null) return builder.buildFuture();

                                            CompoundTag settings = teamData.getCompoundOrEmpty("settings");
                                            for (String key : settings.keySet()) {
                                                if (key.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                    builder.suggest(key);
                                                }
                                            }

                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            assert player != null;

                                            String teamName = datManager.get().getTeam(player.getUUID());
                                            if (teamName != null) {
                                                ListTag blocked = datManager.get()
                                                        .getData()
                                                        .getCompoundOrEmpty("settings")
                                                        .getListOrEmpty("blockTeamsSettings");

                                                for (int i = 0; i < blocked.size(); i++) {
                                                    if (teamName.equalsIgnoreCase(blocked.getString(i).orElse(""))) {
                                                        context.getSource().sendFailure(
                                                                Component.literal(
                                                                        "Server Admin has disabled you from changing your team settings, please contact the Server's Admin!"
                                                                )
                                                        );
                                                        return 0;
                                                    }
                                                }
                                            }

                                            String setting = StringArgumentType.getString(context, "setting");
                                            try {
                                                assert player != null;
                                                datManager.get().handleSettings(player, setting, null);
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                            return 1;
                                        })
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayer();
                                                    assert player != null;

                                                    String teamName = datManager.get().getTeam(player.getUUID());
                                                    if (teamName != null) {
                                                        ListTag blocked = datManager.get()
                                                                .getData()
                                                                .getCompoundOrEmpty("settings")
                                                                .getListOrEmpty("blockTeamsSettings");

                                                        for (int i = 0; i < blocked.size(); i++) {
                                                            if (teamName.equalsIgnoreCase(blocked.getString(i).orElse(""))) {
                                                                context.getSource().sendFailure(
                                                                        Component.literal(
                                                                                "Server Admin has disabled you from changing your team settings, please contact the Server's Admin!"
                                                                        )
                                                                );
                                                                return 0;
                                                            }
                                                        }
                                                    }

                                                    String setting = StringArgumentType.getString(context, "setting");
                                                    boolean value = BoolArgumentType.getBool(context, "value");

                                                    try {
                                                        assert player != null;
                                                        datManager.get().handleSettings(player, setting, value);
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }

                                                    CommandSourceStack source = context.getSource();
                                                    MinecraftServer server = source.getServer();
                                                    teamUtils.rebuildTeams(server);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Setting '" + setting + "' updated to " + value),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(Commands.literal("set")
                                .then(Commands.argument("field", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String prefix = builder.getRemaining();
                                            for (String field : new String[]{"name", "tag", "color", "currencyName", "currencyTag"}) {
                                                if (field.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                    builder.suggest(field);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> {
                                                    String field = StringArgumentType.getString(ctx, "field");
                                                    String prefix = builder.getRemaining();
                                                    if (field.equalsIgnoreCase("color")) {
                                                        for (String color : new String[]{"WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "GREEN", "BROWN", "PURPLE", "BLUE", "GOLD", "RED", "BLACK"}) {
                                                            if (color.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                                builder.suggest(color);
                                                            }
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    String field = StringArgumentType.getString(context, "field");
                                                    String value = StringArgumentType.getString(context, "value");
                                                    ServerPlayer player = context.getSource().getPlayer();

                                                    try {
                                                        assert player != null;
                                                        datManager.get().executeSet(player, field, value);
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }

                                                    CommandSourceStack source = context.getSource();
                                                    MinecraftServer server = source.getServer();
                                                    teamUtils.rebuildTeams(server);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Successfully updated  " + field),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(Commands.literal("createCurrency")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("tag", StringArgumentType.string())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayer();
                                                    assert player != null;

                                                    String teamName = datManager.get().getTeam(player.getUUID());
                                                    if (teamName == null) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("You are not in a team!")
                                                        );
                                                        return 0;
                                                    }

                                                    CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");
                                                    CompoundTag teamData = teams.getCompoundOrEmpty(teamName);
                                                    String ownerStr = teamData.getString("owner").orElse("");

                                                    if (!ownerStr.equalsIgnoreCase(player.getUUID().toString())) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("Only the team leader can create a currency!")
                                                        );
                                                        return 0;
                                                    }

                                                    String currencyName = StringArgumentType.getString(context, "name");
                                                    String currencyTagRaw = StringArgumentType.getString(context, "tag");

                                                    String existingCurrencyName = teamData.getString("currencyName").orElse("");
                                                    if (!existingCurrencyName.isEmpty()) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("Your team already has a currency! Use '/lixteams set currencyName' or '/lixteams set currencyTag' to modify it, or '/lixteams removeCurrency' to remove it first.")
                                                        );
                                                        return 0;
                                                    }

                                                    for (String existingTeamName : teams.keySet()) {
                                                        CompoundTag existingTeam = teams.getCompoundOrEmpty(existingTeamName);
                                                        String existingName = existingTeam.getString("currencyName").orElse("");
                                                        if (existingName.equalsIgnoreCase(currencyName)) {
                                                            context.getSource().sendFailure(
                                                                    Component.literal("A currency with this name already exists!")
                                                            );
                                                            return 0;
                                                        }
                                                    }

                                                    for (String existingTeamName : teams.keySet()) {
                                                        CompoundTag existingTeam = teams.getCompoundOrEmpty(existingTeamName);
                                                        ListTag existingCurrencyTag = existingTeam.getListOrEmpty("currencyTag");
                                                        StringBuilder tagBuilder = new StringBuilder();
                                                        for (int i = 0; i < existingCurrencyTag.size(); i++) {
                                                            tagBuilder.append(existingCurrencyTag.getString(i).orElse(""));
                                                        }
                                                        if (tagBuilder.toString().equalsIgnoreCase(currencyTagRaw)) {
                                                            context.getSource().sendFailure(
                                                                    Component.literal("A currency with this tag already exists!")
                                                            );
                                                            return 0;
                                                        }
                                                    }

                                                    ListTag currencyTag = new ListTag();
                                                    for (char c : currencyTagRaw.toCharArray()) {
                                                        currencyTag.add(StringTag.valueOf(String.valueOf(c)));
                                                    }

                                                    try {
                                                        datManager.get().setTeamCurrencyName(teamName, currencyName);
                                                        datManager.get().setTeamCurrencyTag(teamName, currencyTag);
                                                    } catch (IOException e) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("Failed to save currency data.")
                                                        );
                                                        e.printStackTrace();
                                                        return 0;
                                                    }

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Currency created: ")
                                                                    .append(Component.literal(currencyName).withStyle(ChatFormatting.YELLOW))
                                                                    .append(Component.literal(" [")
                                                                    .append(Component.literal(currencyTagRaw).withStyle(ChatFormatting.GOLD))
                                                                    .append(Component.literal("]"))
                                                                    .withStyle(ChatFormatting.YELLOW)),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(Commands.literal("listCurrencies")
                                .executes(context -> {
                                    CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");
                                    MutableComponent message = Component.literal("Available Currencies\n").withStyle(ChatFormatting.GOLD);

                                    boolean hasCurrencies = false;
                                    for (String teamName : teams.keySet()) {
                                        CompoundTag teamData = teams.getCompoundOrEmpty(teamName);
                                        String currencyName = teamData.getString("currencyName").orElse("");
                                        ListTag currencyTag = teamData.getListOrEmpty("currencyTag");

                                        if (!currencyName.isEmpty() || currencyTag.size() > 0) {
                                            hasCurrencies = true;
                                            StringBuilder tagBuilder = new StringBuilder();
                                            for (int i = 0; i < currencyTag.size(); i++) {
                                                tagBuilder.append(currencyTag.getString(i).orElse(""));
                                            }
                                            Component currencyEntry = Component.literal("")
                                                    .withStyle(ChatFormatting.YELLOW)
                                                    .append(Component.literal(currencyName.isEmpty() ? "(unnamed)" : currencyName).withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(" [")).withStyle(ChatFormatting.YELLOW)
                                                    .append(Component.literal(tagBuilder.toString().isEmpty() ? "?" : tagBuilder.toString()).withStyle(ChatFormatting.GOLD))
                                                    .append(Component.literal("] by ")).withStyle(ChatFormatting.YELLOW)
                                                    .append(Component.literal(teamName).withStyle(ChatFormatting.WHITE))
                                                    .append(Component.literal("\n")).withStyle(ChatFormatting.YELLOW);

                                            message.append(currencyEntry);
                                        }
                                    }

                                    if (!hasCurrencies) {
                                        message.append(Component.literal("No currencies have been created yet.").withStyle(ChatFormatting.GRAY));
                                    }

                                    context.getSource().sendSuccess(() -> message, false);
                                    return 1;
                                })
                        )

                        .then(Commands.literal("removeCurrency")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    assert player != null;

                                    String teamName = datManager.get().getTeam(player.getUUID());
                                    if (teamName == null) {
                                        context.getSource().sendFailure(
                                                Component.literal("You are not in a team!")
                                        );
                                        return 0;
                                    }

                                    CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");
                                    CompoundTag teamData = teams.getCompoundOrEmpty(teamName);
                                    String ownerStr = teamData.getString("owner").orElse("");

                                    if (!ownerStr.equalsIgnoreCase(player.getUUID().toString())) {
                                        context.getSource().sendFailure(
                                                Component.literal("Only the team leader can remove a currency!")
                                        );
                                        return 0;
                                    }

                                    String existingCurrencyName = teamData.getString("currencyName").orElse("");
                                    if (existingCurrencyName.isEmpty()) {
                                        context.getSource().sendFailure(
                                                Component.literal("Your team does not have a currency!")
                                        );
                                        return 0;
                                    }

                                    try {
                                        teamData.putString("currencyName", "");
                                        teamData.put("currencyTag", new ListTag());

                                        datManager.get().getData().put("teams", teams);
                                        datManager.get().save();
                                    } catch (IOException e) {
                                        context.getSource().sendFailure(
                                                Component.literal("Failed to remove currency data.")
                                        );
                                        e.printStackTrace();
                                        return 0;
                                    }

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Currency removed successfully!"),
                                            false
                                    );
                                    return 1;
                                })
                        )

                        .then(Commands.literal("generateCurrency")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            assert player != null;

                                            String teamName = datManager.get().getTeam(player.getUUID());
                                            if (teamName == null) {
                                                context.getSource().sendFailure(
                                                        Component.literal("You are not in a team!")
                                                );
                                                return 0;
                                            }

                                            CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");
                                            CompoundTag teamData = teams.getCompoundOrEmpty(teamName);
                                            String ownerStr = teamData.getString("owner").orElse("");

                                            if (!ownerStr.equalsIgnoreCase(player.getUUID().toString())) {
                                                context.getSource().sendFailure(
                                                        Component.literal("Only the team leader can generate currency!")
                                                );
                                                return 0;
                                            }

                                            String currencyName = teamData.getString("currencyName").orElse("");
                                            if (currencyName.isEmpty()) {
                                                context.getSource().sendFailure(
                                                        Component.literal("Your team does not have a currency!")
                                                );
                                                return 0;
                                            }

                                            int amount = IntegerArgumentType.getInteger(context, "amount");

                                            if (datManager.get().hasPendingCurrency(teamName, player.getUUID().toString())) {
                                                long completionTime = datManager.get().getPendingCompletionTime(teamName, player.getUUID().toString());
                                                long remainingSeconds = completionTime - (System.currentTimeMillis() / 1000);
                                                long remainingMinutes = remainingSeconds / 60;
                                                long remainingHours = remainingMinutes / 60;

                                                if (remainingHours > 0) {
                                                    context.getSource().sendFailure(
                                                            Component.literal("Your currency is still being generated! " + remainingHours + "h " + remainingMinutes + "m remaining.")
                                                                    .withStyle(ChatFormatting.RED)
                                                    );
                                                } else {
                                                    context.getSource().sendFailure(
                                                            Component.literal("Your currency is still being generated! " + remainingMinutes + "m remaining.")
                                                                    .withStyle(ChatFormatting.RED)
                                                    );
                                                }
                                                return 0;
                                            }

                                            long delaySeconds = datManager.get().getTeamDelaySeconds(teamName);

                                            if (delaySeconds == 0) {
                                                try {
                                                    datManager.get().addPlayerCurrencyBalance(player.getUUID().toString(), currencyName, amount);
                                                } catch (IOException e) {
                                                    context.getSource().sendFailure(
                                                            Component.literal("Failed to generate currency.")
                                                    );
                                                    e.printStackTrace();
                                                    return 0;
                                                }

                                                String currencyTagStr = datManager.get().getTeamCurrencyTagString(teamName);
                                                Component generatedMsg = Component.literal("Generated ")
                                                        .withStyle(ChatFormatting.YELLOW)
                                                        .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" "))
                                                        .append(Component.literal(currencyName).withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" ["))
                                                        .append(Component.literal(currencyTagStr).withStyle(ChatFormatting.GOLD))
                                                        .append(Component.literal("]"))
                                                        .append(Component.literal(" for yourself!"));

                                                context.getSource().sendSuccess(() -> generatedMsg, false);

                                                MinecraftServer server = context.getSource().getServer();
                                                String playerName = player.getGameProfile().name();
                                                Component globalMsg = Component.literal("")
                                                        .append(Component.literal(playerName).withStyle(ChatFormatting.GREEN))
                                                        .append(Component.literal(" generated "))
                                                        .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" "))
                                                        .append(Component.literal(currencyName).withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" ["))
                                                        .append(Component.literal(currencyTagStr).withStyle(ChatFormatting.GOLD))
                                                        .append(Component.literal("]"))
                                                        .append(Component.literal(" for team "))
                                                        .append(Component.literal(teamName).withStyle(ChatFormatting.WHITE))
                                                        .append(Component.literal("!"));

                                                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                                    p.sendSystemMessage(globalMsg);
                                                }

                                                return 1;
                                            }

                                            long completionTime = System.currentTimeMillis() / 1000 + delaySeconds;
                                            try {
                                                datManager.get().addPendingCurrency(teamName, player.getUUID().toString(), currencyName, amount, completionTime);
                                            } catch (IOException e) {
                                                context.getSource().sendFailure(
                                                        Component.literal("Failed to schedule currency generation.")
                                                );
                                                e.printStackTrace();
                                                return 0;
                                            }

                                            String currencyTagStr = datManager.get().getTeamCurrencyTagString(teamName);
                                            long remainingMinutes = delaySeconds / 60;
                                            long remainingHours = remainingMinutes / 60;

                                            Component generatedMsg = Component.literal("Currency is being generated! ")
                                                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(" "))
                                                    .append(Component.literal(currencyName).withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(" ["))
                                                    .append(Component.literal(currencyTagStr).withStyle(ChatFormatting.GOLD))
                                                    .append(Component.literal("]"))
                                                    .append(Component.literal(" in "))
                                                    .append(Component.literal(remainingHours > 0 ? remainingHours + "h " + remainingMinutes % 60 + "m" : remainingMinutes + "m").withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal("!"));

                                            context.getSource().sendSuccess(() -> generatedMsg, false);

                                            MinecraftServer server = context.getSource().getServer();
                                            String playerName = player.getGameProfile().name();
                                            Component globalMsg = Component.literal("")
                                                    .append(Component.literal(playerName).withStyle(ChatFormatting.GREEN))
                                                    .append(Component.literal(" is generating "))
                                                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(" "))
                                                    .append(Component.literal(currencyName).withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(" ["))
                                                    .append(Component.literal(currencyTagStr).withStyle(ChatFormatting.GOLD))
                                                    .append(Component.literal("]"))
                                                    .append(Component.literal(" for team "))
                                                    .append(Component.literal(teamName).withStyle(ChatFormatting.WHITE))
                                                    .append(Component.literal("!"));
                                            String currencyGenerationServerMessage = String.format(
                                                    "%s is generating %s %s [%s]",
                                                    playerName,
                                                    amount,
                                                    currencyName,
                                                    currencyTagStr
                                                    );

                                            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                                p.sendSystemMessage(globalMsg);
                                            }
                                            LOGGER.info(currencyGenerationServerMessage);

                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("kick")
                                .then(Commands.argument("playerName", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            ServerPlayer owner = context.getSource().getPlayer();
                                            String prefix = builder.getRemaining();
                                            if (owner == null) return builder.buildFuture();

                                            CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");
                                            String ownerStr = owner.getUUID().toString();
                                            CompoundTag teamData = null;

                                            for (String tName : teams.keySet()) {
                                                CompoundTag t = teams.getCompoundOrEmpty(tName);
                                                if (ownerStr.equals(t.getString("owner").orElse(""))) {
                                                    teamData = t;
                                                    break;
                                                }
                                            }

                                            if (teamData == null) return builder.buildFuture();
                                            MinecraftServer server = context.getSource().getServer();
                                            ListTag members = teamData.getListOrEmpty("members");
                                            for (int i = 0; i < members.size(); i++) {
                                                String memberUUID = members.getString(i).orElse("");
                                                if (!ownerStr.equals(memberUUID)) {
                                                    ServerPlayer member = server.getPlayerList().getPlayer(UUID.fromString(memberUUID));

                                                    if (member != null && member.getGameProfile().name().toLowerCase().startsWith(prefix.toLowerCase())) {
                                                        builder.suggest(member.getGameProfile().name());
                                                    }
                                                }
                                            }

                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer owner = context.getSource().getPlayer();
                                            if (owner == null) return 0;

                                            String targetName = StringArgumentType.getString(context, "playerName");
                                            MinecraftServer server = context.getSource().getServer();
                                            ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
                                            if (target == null) {
                                                context.getSource().sendFailure(Component.literal("Player not found or not online!"));
                                                return 0;
                                            }

                                            try {
                                                datManager.get().kickMember(owner, target);
                                            } catch (IOException e) {
                                                context.getSource().sendFailure(Component.literal("Failed to save team data."));
                                                e.printStackTrace();
                                                return 0;
                                            }

                                            teamUtils.rebuildTeams(server);

                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("currency", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            String prefix = builder.getRemaining();
                                            if (player != null) {
                                                String playerUuid = player.getUUID().toString();
                                                List<String> currencies = datManager.get().getPlayerCurrencies(playerUuid);
                                                for (String currencyName : currencies) {
                                                    if (currencyName.toLowerCase().startsWith(prefix.toLowerCase())) {
                                                        builder.suggest(currencyName);
                                                    }
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayer();
                                                    assert player != null;

                                                    String teamName = datManager.get().getTeam(player.getUUID());
                                                    if (teamName == null) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("You are not in a team!")
                                                        );
                                                        return 0;
                                                    }

                                                    String requestedCurrency = StringArgumentType.getString(context, "currency");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    String playerUuid = player.getUUID().toString();

                                                    long balance = datManager.get().getPlayerCurrencyBalance(playerUuid, requestedCurrency);
                                                    if (balance < amount) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("You do not have enough " + requestedCurrency + "! You have " + balance + ", but need " + amount + ".")
                                                        );
                                                        return 0;
                                                    }

                                                    try {
                                                        datManager.get().subtractPlayerCurrencyBalance(playerUuid, requestedCurrency, amount);

                                                        ItemStack withdrawalPaper = new ItemStack(Items.PAPER);
                                                        String currencyTagStr = datManager.get().getTeamCurrencyTagString(teamName);
                                                        withdrawalPaper.set(DataComponents.CUSTOM_NAME, Component.literal("[" + currencyTagStr + "] " + amount));

                                                        if (player.getInventory().add(withdrawalPaper)) {
                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal("Withdrew " + amount + " " + requestedCurrency + " to a currency withdrawal paper!"),
                                                                    false
                                                            );
                                                        } else {
                                                            context.getSource().sendFailure(
                                                                    Component.literal("Your inventory is full! Cannot create withdrawal paper.")
                                                            );
                                                            datManager.get().addPlayerCurrencyBalance(playerUuid, requestedCurrency, amount);
                                                        }
                                                    } catch (IOException e) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("Failed to process withdrawal: " + e.getMessage())
                                                        );
                                                        e.printStackTrace();
                                                        return 0;
                                                    }

                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(Commands.literal("balance")
                                .then(Commands.argument("playerName", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String prefix = builder.getRemaining();
                                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                                    .filter(player -> player.getGameProfile().name().toLowerCase().startsWith(prefix.toLowerCase()))
                                                    .forEach(player -> builder.suggest(player.getGameProfile().name()));
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

                                            return displayBalance(context.getSource(), targetPlayer);
                                        })
                                )
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    assert player != null;
                                    return displayBalance(context.getSource(), player);
                                })
                        )

        ));

        LOGGER.info("Commands Registered!");
    }

    private static int displayBalance(CommandSourceStack source, ServerPlayer player) {
        String playerUuid = player.getUUID().toString();
        List<String> currencies = datManager.get().getPlayerCurrencies(playerUuid);

        MutableComponent message = Component.literal("Balance for ")
                .append(Component.literal(player.getGameProfile().name()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("\n").withStyle(ChatFormatting.GOLD));

        if (currencies.isEmpty()) {
            message.append(Component.literal("No currencies found.").withStyle(ChatFormatting.GRAY));
        } else {
            for (String currencyName : currencies) {
                long balance = datManager.get().getPlayerCurrencyBalance(playerUuid, currencyName);
                String currencyTag = findCurrencyTag(currencyName);

                Component currencyEntry = Component.literal("")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(currencyName).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(": "))
                        .append(Component.literal(String.valueOf(balance)).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" ["))
                        .append(Component.literal(currencyTag).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("]"))
                        .append(Component.literal("\n"))
                        .withStyle(ChatFormatting.YELLOW);

                message.append(currencyEntry);
            }
        }

        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static String findCurrencyTag(String currencyName) {
        CompoundTag teams = datManager.get().getData().getCompoundOrEmpty("teams");
        for (String teamName : teams.keySet()) {
            CompoundTag teamData = teams.getCompoundOrEmpty(teamName);
            String name = teamData.getString("currencyName").orElse("");
            if (name.equalsIgnoreCase(currencyName)) {
                ListTag currencyTag = teamData.getListOrEmpty("currencyTag");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < currencyTag.size(); i++) {
                    sb.append(currencyTag.getString(i).orElse(""));
                }
                return sb.toString();
            }
        }
        return "?";
    }
}
