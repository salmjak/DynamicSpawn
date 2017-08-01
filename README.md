# DynamicSpawn

#For Pixelmon 5.1.2 - Minecraft 1.10.2 - Sponge API 5.1.0#

A simple plugin which dynamically increases the spawn limits (linearly) in Pixelmon based on number of online players (based on the initial max limit set in the pixelmon config for each category and the max number of allowed online players on the server).

Use the .cfg to set the minimum limit (default 0.20 (20% of max limit))

TODO:

- Rework spawn system to have limits per world instead of per player.
        - Retrieve clusters of players, take their average position and create a spawning area around that point. Spread out             spawning based on the amount of players in a specific spawn area.
