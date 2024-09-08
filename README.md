Sometimes mobs can randomly pick up loot from other dead mobs without any player actions and become persistent. Those mobs then can pile up in unexpected places with little to no chances of player encounter or environmental death.\
Persistent mobs are not counted towards mobcap and continue to accumulate thus contributing to lag, but even if they do count they would drastically reduce other mobs spawning.\
This plugin aims to solve this issue without disabling any mechanics or enabling any restriction, but just by detecting persistent naturally spawned mobs which lived for too long without a nametag and unpersist them.\
Upon despawning all mob items will be dropped to reduce chances of losing something valuable for players.

Works for every monster with the following exceptions:
- Nametagged by player or in a team with visible nametag
- Unnaturally spawned mobs because they generated within structures
- Custom and summoned mobs to not accidentally break other plugins behaviour

General recomendations to saveup even more:
- Endermans with block in hands are handled by Purpur
- Zombified Piglins from portals are disabled by Spigot
