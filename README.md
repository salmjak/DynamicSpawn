# DynamicSpawn

#For Pixelmon 5.1.2 - Minecraft 1.10.2 - Sponge API 5.1.0#

A simple plugin which dynamically increases the spawn limits of Pixelmon. The plugin will increase spawns when players are aggregated using a function based on the number of loaded chunks and online players but never go above the max limit set in pixelmon configs.

Use the .cfg to set the minimum limit (default 0.20 (20% of max limit))

TODO:

- Rework spawn system to have limits per player instead of area.
       
       - Cluster players with locality sensitive hashing.
