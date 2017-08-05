package hobbyist.samIam.dynamicspawn;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.chunkSpawnRadius;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.maxSpawnsPerTick;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import hobbyist.samIam.utilities.EntitiesUtility;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.world.World;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.entity.ConstructEntityEvent;
import org.spongepowered.api.event.entity.ConstructEntityEvent.Post;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.ExpireEntityEvent;
import org.spongepowered.api.event.filter.type.Exclude;
import org.spongepowered.api.scheduler.SpongeExecutorService;


@Plugin
(
        id = "dynamicspawn",
        name = "DynamicSpawn",
        version = "0.0.3",
        dependencies = @Dependency(id = "pixelmon"),
        description = "Limits the spawn of Pokemon dynamically.",
        authors = "samIam"
        
)
public class DynamicSpawn {
    
    // Setup Logger
    @Inject
    public Logger log;
    
    public static DynamicSpawn instance;
    
    public static Logger getLogger()
    {
        return instance.log;
    }
    
    public int maxOnServer = 200;
    public int maxPerArea = 60;
    public int maxPerPlayer = 20;
    public int spawnRadius = 8;
    public int maxOnlinePlayers = 20;
    
    @Inject
    @DefaultConfig(sharedRoot = true)
    public Path configPath;
    
    public ConfigurationLoader<CommentedConfigurationNode> configLoader;
    
    public World w;
    SpongeExecutorService schedule;
    
    public int spawnedPixelmon = 0;
    
    //Load files
    @Listener
    public void onInitialization(GameInitializationEvent event){
        instance = this;
        
        //Load configs
        configLoader = HoconConfigurationLoader.builder().setPath(configPath).build();
        CommentedConfigurationNode rootNode;
        try
        {
            if(!Files.exists(configPath)){
                rootNode = configLoader.load();
                rootNode.getNode("Limits", "OnServer").setValue(200);
                rootNode.getNode("Limits", "PerArea").setValue(60);
                rootNode.getNode("Limits", "PerPlayer").setValue(20);
                configLoader.save(rootNode);
            }
            else
            {
                rootNode = configLoader.load();
                maxOnServer = rootNode.getNode("Limits", "OnServer").getInt(200);
                maxPerArea = rootNode.getNode("Limits", "PerArea").getInt(60);
                maxPerPlayer = rootNode.getNode("Limits", "PerPlayer").getInt(20);
            } 
        } catch(IOException e) {
            log.error("Failed to create/load or save config" + e.getMessage());
        }
    }
    
    @Listener
    public void onServerStart(GameStartedServerEvent event) 
    {
        spawnRadius = chunkSpawnRadius;
        maxOnlinePlayers = Sponge.getServer().getMaxPlayers();
        maxSpawnsPerTick = 0;
        
        w = Sponge.getServer().getWorlds().toArray(new World[0])[0];
        
        //Disable spawn chunks loaded
        w.getProperties().setKeepSpawnLoaded(false);
        schedule = Sponge.getScheduler().createSyncExecutor(this);
        schedule.scheduleAtFixedRate(new LogStatus(), 30, 30, TimeUnit.SECONDS);
        
        
    }
    
    class LogStatus implements Runnable
    {

        @Override
        public void run() 
        {
            log.info("Currently " + spawnedPixelmon + " pokemon spawned.");
        }
    }
    
    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event)
    {
        maxSpawnsPerTick = (Sponge.getServer().getOnlinePlayers().size()*maxPerPlayer)-spawnedPixelmon;
        log.info("Player joined. Adjusting max spawns per tick to: " + maxSpawnsPerTick);
    }
    
    @Listener
    public void onPlayerLeave(ClientConnectionEvent.Disconnect event)
    {
        maxSpawnsPerTick = (Sponge.getServer().getOnlinePlayers().size()*maxPerPlayer)-spawnedPixelmon;
        log.info("Player left. Adjusting max spawns per tick to: " + maxSpawnsPerTick);
    }

    
    @Listener
    @Exclude(SpawnEntityEvent.Spawner.class)
    public void onSpawn(SpawnEntityEvent event)
    {
        for(Entity e : event.getEntities())
        {
            if(!(e instanceof EntityPixelmon))
            {
                continue;
            }

            if(spawnedPixelmon >= maxOnServer)
            {
                e.remove();
                continue;
            }
            //Distance between a player and possible spawn will be the radius * chunkSize.
            ArrayList<Player> nearbyPlayers = EntitiesUtility.getNearbyPlayers(e, spawnRadius);
            if(nearbyPlayers.isEmpty())
            {
                EntityPixelmon poke = (EntityPixelmon)e;
                e.remove();
                continue;
            }

            Vector3d avg_pos = new Vector3d(0,0,0);
            Vector3d minPos = nearbyPlayers.get(0).getTransform().getPosition();
            Vector3d maxPos = nearbyPlayers.get(0).getTransform().getPosition();

            for(Player p : nearbyPlayers)
            {
                avg_pos = avg_pos.add(p.getTransform().getPosition());
                if(p.getTransform().getPosition().getX() < minPos.getX() || p.getTransform().getPosition().getZ() > minPos.getZ())
                {
                    //New pos is more south-west
                    minPos = p.getTransform().getPosition();
                } 
                else if(p.getTransform().getPosition().getX() > maxPos.getX() || p.getTransform().getPosition().getZ() < maxPos.getZ())
                {
                    //New pos is more north-east
                    maxPos = p.getTransform().getPosition();
                }
            }

            //Divide by 16 to get number of chunks between the player furthest to south-west and player furthest to north-east
            double addToViewDistance = EntitiesUtility.getEuclidianDistance(minPos, maxPos)/16.0;
            avg_pos = avg_pos.div((double)nearbyPlayers.size());

            //10 is the default server view distance
            int pokemonInAreaAroundPlayers = EntitiesUtility.getNumberOfPokemonWithinViewDistanceOfPos(avg_pos, 10.0 + addToViewDistance);
            
            EntityPixelmon p = (EntityPixelmon)e;
            if(pokemonInAreaAroundPlayers >= maxPerArea)
            {
                e.remove();
            } 
            else if(pokemonInAreaAroundPlayers >= maxPerPlayer*nearbyPlayers.size())
            {
                e.remove();
            } 
            else 
            {
                //spawnedPixelmon.add(p.getUniqueID());
                spawnedPixelmon++;
                log.info("Spawned a " + p.getName());
                maxSpawnsPerTick = (Sponge.getServer().getOnlinePlayers().size()*maxPerPlayer)-spawnedPixelmon;
            }
        }
    }
    
    @Listener
    public void onDespawn(ExpireEntityEvent event)
    {
        if(event.getTargetEntity() instanceof EntityPixelmon)
        {
            spawnedPixelmon--;
        }
    }
    
    @Listener
    public void onServerStopped(GameStoppedServerEvent event)
    {
        
    }
    
}
