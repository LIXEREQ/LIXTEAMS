> [!NOTE]
> **Original mod: https://github.com/bba5696/Allied**
<hr>

[![Fabric API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/requires/fabric-api_64h.png)](https://modrinth.com/mod/fabric-api)

# LIXTEAMS

**This mod is a fork of Allied that adds an economy system**

**Because i dislike java the new code is mostly written by Gemma 4 26B A4B and Qwen 3.6 35B A6B running locally on my second GPU**
**If you want new features or have other issues with this mod you can make a GitHub issue**

**This mod allows players to create and make teams, featuring friendly fire, Seeing teamates while there invisible. all togglable in settings, these features are easily accessed with `/lixteams`**
<hr>

# Commands

- `/lixteams create <teamName> <teamTag>` Create a new team and become its owner
- `/lixteams disband` Disbands the team **(Team Owner Command)**
- `/lixteams leave` Leave the current team you are in
- `/lixteams join <teamName>` Send a join request to the team owner
- `/lixteams accept <playerName>` Accept the players join request **(Team Owner Command)**
- `/lixteams deny <playerName>` Deny the players join request **(Team Owner Command)**
- `/lixteams invite <playerName>` Send a team invitation to a player for them to join **(Team Owner Command)**
- `/lixteams invAccept <teamName>` Accept the owner invite
- `/lixteams invDeny <teamName>` Deny the owners invite
- `/lixteams info` Show info about the team, Team Name, Team Tag, Owner, Members
- `/lixteams settings` Shows the teams settings available and buttons to change them **(Team Owner Command)**
- `/lixteams set <teamName|teamTag|teamColor> <value>` Chose a value to change and set a name, tag or color **(Team Owner Command)**
- `/lixteams kick <playerName>` Kick a player from the team **(Team Owner Command)**
- `/lixteams tm` Toggle team chat

# Admin Commands

- `/lixteamsAdmin memberCap <value>` Set a new max members in a team
- `/lixteamsAdmin info <teamName>` Get the info of any valid team
- `/lixteamsAdmin list` List all teams in the server
- `/lixteamsAdmin reset [<Code>]` A command to wipe all mod data and reset it to default, after entering the command, a code thats valid for 60s will be given to confirm the reset
- `/lixteamsAdmin blockSettings <teamName> <boolean>` Prevents the team's owner from changing their settings
- `/lixteamsAdmin modifySettings <teamName> <settings> <boolean>` Modifies the settings of a team

<hr>
