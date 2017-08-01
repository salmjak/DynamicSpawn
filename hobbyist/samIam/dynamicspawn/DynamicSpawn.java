package hobbyist.samIam.dynamicspawn;

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
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.game.state.GameInitializationEvent;


@Plugin
(
        id = "dynamicspawn",
        name = "DynamicSpawn",
        version = "0.0.1",
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
    }
    
    @Listener
    public void onServerStopped(GameStoppedServerEvent event)
    {

    }
    
    @Listener
    public void onPlayerJoin(Join event)
    {
       log.info("A player joined. Adjusting spawn limit.");
       ChangeLimits();
    }
    
    @Listener
    public void onPlayerLeave(Disconnect event)
    {
        log.info("A player leaved. Adjusting spawn limit.");
        ChangeLimits();
    }
    
    
    void ChangeLimits()
    {
        double rate = (double)Sponge.getServer().getOnlinePlayers().size()/(double)maxServerPlayers;
        maxNumLandPokemon = Math.min((int)Math.ceil((initMaxLand*minPercentage) + (initMaxLand*rate)), initMaxLand);
        log.info("Land: " + maxNumLandPokemon);
        
        maxNumWaterPokemon = Math.min((int)Math.ceil((initMaxWater*minPercentage) + (initMaxWater*rate)), initMaxWater);
        log.info("Water: " + maxNumWaterPokemon);
        
        maxNumAirPokemon = Math.min((int)Math.ceil((initMaxAir*minPercentage) + (initMaxAir*rate)), initMaxAir);
        log.info("Air: " + maxNumAirPokemon);
        
        maxNumUndergroundPokemon = Math.min((int)Math.ceil((initMaxUnderground*minPercentage) + (initMaxUnderground*rate)), initMaxUnderground);
        log.info("Undergound: " + maxNumUndergroundPokemon);
        
        maxNumBosses = Math.min((int)Math.ceil((initMaxBosses*minPercentage) + (initMaxBosses*rate)), initMaxBosses);
        log.info("Bosses: " + maxNumBosses);
        
        maxNumNPCs = Math.min((int)Math.ceil((initMaxNPCs*minPercentage) + (initMaxNPCs*rate)), initMaxNPCs);
        log.info("NPCs: " + maxNumNPCs);
    }
}
