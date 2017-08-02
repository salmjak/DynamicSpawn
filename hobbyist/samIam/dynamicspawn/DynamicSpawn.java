package hobbyist.samIam.dynamicspawn;

import com.google.common.collect.Lists;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent.Join;
import org.spongepowered.api.event.network.ClientConnectionEvent.Disconnect;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.maxNumLandPokemon;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.maxNumWaterPokemon;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.maxNumAirPokemon;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.maxNumUndergroundPokemon;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.maxNumBosses;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.maxNumNPCs;
import static java.io.File.separator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.world.World;


@Plugin
(
        id = "dynamicspawn",
        name = "DynamicSpawn",
        version = "0.0.2",
        dependencies = @Dependency(id = "pixelmon"),
        description = "Increase the spawn limits in Pixelmon based on number of online players.",
        authors = "samIam"
        
)
public class DynamicSpawn {
    
    // Setup Logger
    private static final String name = "DynamicSpawn";
    public static final Logger log = LoggerFactory.getLogger(name);
    
    public int initMaxLand = 0;
    public int initMaxAir = 0;
    public int initMaxWater = 0;
    public int initMaxUnderground = 0;
    public int initMaxBosses = 0;
    public int initMaxNPCs = 0;
    public double minPercentage = 0.20;
    public int maxServerPlayers = 0;
    
    public Path configPath = Paths.get("config" + separator + "DynamicSpawn.cfg");
    public ConfigurationLoader<CommentedConfigurationNode> configLoader = HoconConfigurationLoader.builder().setPath(configPath).build();
    
    World w;
    SpongeExecutorService scheduler;
    
    //Load files
    @Listener
    public void onInitialization(GameInitializationEvent event){
        //Load configs
        CommentedConfigurationNode rootNode;
        try
        {
            if(!Files.exists(configPath)){
                rootNode = configLoader.load();
                rootNode.getNode("Minimum Limit", "Percentage").setValue(0.20).setComment("The minimum amount as a percentage of the max limit for each category.");
                configLoader.save(rootNode);
            }
            else
            {
                rootNode = configLoader.load();
                minPercentage = rootNode.getNode("Minimum Amount", "Percentage").getDouble();
            } 
        } catch(IOException e) {
            log.error("Failed to create config" + e.getMessage());
        }
    }
    
    @Listener
    public void onServerStart(GameStartedServerEvent event) 
    {
        initMaxLand = maxNumLandPokemon;
        initMaxAir = maxNumAirPokemon;
        initMaxWater = maxNumWaterPokemon;
        initMaxUnderground = maxNumUndergroundPokemon;
        initMaxBosses = maxNumBosses;
        initMaxNPCs = maxNumNPCs;
        maxServerPlayers = Sponge.getServer().getMaxPlayers();
        
        w = Sponge.getServer().getWorlds().toArray(new World[0])[0];
        
        //Disable spawn chunks loaded
        w.getProperties().setKeepSpawnLoaded(false);
        
        scheduler = Sponge.getScheduler().createSyncExecutor(this);
        //Update spawn rates every 10 seconds.
        scheduler.scheduleAtFixedRate(new ChangeLimits(), 0, 10, TimeUnit.SECONDS);
    }
    
    @Listener
    public void onServerStopped(GameStoppedServerEvent event)
    {
        scheduler.shutdown();
    }
    
    class ChangeLimits implements Runnable
    {
        @Override
        public void run() {
            int loaded_chunks = Lists.newArrayList(w.getLoadedChunks()).size();
            int players = Sponge.getServer().getOnlinePlayers().size();
            //Default view distance for each player is 10 (21*21 chunks loaded).
            //chunkPerPlayer is low when a lot of players are at the same position.
            double chunkPerPlayer = (double)loaded_chunks/(double)(21*21*players);
            double rate = (1.0/chunkPerPlayer)-1.0; //should be zero when all players are evenly spread out.
            maxNumLandPokemon = Math.min((int)Math.ceil((initMaxLand*minPercentage) + rate), initMaxLand);
            log.info("Land: " + maxNumLandPokemon);

            maxNumWaterPokemon = Math.min((int)Math.ceil((initMaxWater*minPercentage) + rate), initMaxWater);
            log.info("Water: " + maxNumWaterPokemon);

            maxNumAirPokemon = Math.min((int)Math.ceil((initMaxAir*minPercentage) + rate), initMaxAir);
            log.info("Air: " + maxNumAirPokemon);

            maxNumUndergroundPokemon = Math.min((int)Math.ceil((initMaxUnderground*minPercentage) + rate), initMaxUnderground);
            log.info("Undergound: " + maxNumUndergroundPokemon);

            maxNumBosses = Math.min((int)Math.ceil((initMaxBosses*minPercentage) + rate), initMaxBosses);
            log.info("Bosses: " + maxNumBosses);

            maxNumNPCs = Math.min((int)Math.ceil((initMaxNPCs*minPercentage) + rate), initMaxNPCs);
            log.info("NPCs: " + maxNumNPCs);
        }
    }
}
