# Packwiz server updater
Updates packwiz modpacks using simple in-game commands.

[![modrinth badge](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/packwiz-server-updater)

### (Neo)Forge Versions
The mod should work if you install [Sinytra's Connector](https://modrinth.com/mod/connector) alongside it

## Usage
- Make a [Packwiz modpack](https://packwiz.infra.link/)
- Use `/packwiz link [URL]` in-game to link to your `pack.toml` file
- Update the modpack using `/packwiz update`

## Singleplayer vs Dedicated Server
- Starting from version 3 singleplayer and LAN worlds are supported.
- Version 2 only supports updating from a dedicated server. This was an oversight and might be fixed later.

## Permissions
- As of version 3 you can now set a [minimum permission level](https://minecraft.wiki/w/Permission_level) for calling the `/packwiz` command. By default this is set to 4 (Operator)
- This may be useful if you who want to give regular players (level 0) or those in creative (level 2) an option to link and update the modpack.


