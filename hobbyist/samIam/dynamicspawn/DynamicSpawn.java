package hobbyist.samIam.dynamicspawn;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.chunkSpawnRadius;
import static com.pixelmonmod.pixelmon.config.PixelmonConfig.maxSpawnsPerTick;
import com.pixelmonmod.pixelmon.database.SpawnLocation;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.util.Tuple;


@Plugin
(
        id = "dynamicspawn",
        name = "DynamicSpawn",
        version = "0.0.8",
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
    int actualMaxPerPlayer = 20;
    public int spawnRadius = 8;
    public int maxOnlinePlayers = 20;
    public double maxFlyingPercentage = 0.10;
    public double maxWaterPercentage = 0.30;
    public boolean alwaysLegendaries = true;
    public boolean alwaysBosses = true;
    
    @Inject
    @DefaultConfig(sharedRoot = true)
    public Path configPath;
    
    public ConfigurationLoader<CommentedConfigurationNode> configLoader;
    
    public World w;
    SpongeExecutorService scheduleLog;
    SpongeExecutorService scheduleCleanUp;
    
    public  HashSet<UUID> spawnedPixelmon = new HashSet<>();
    
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
                rootNode.getNode("Limits", "maxFlyingPercentage").setValue(0.10);
                rootNode.getNode("Limits", "maxWaterPercentage").setValue(0.30);
                rootNode.getNode("Limits", "alwaysAllowLegendaries").setValue(true);
                rootNode.getNode("Limits", "alwaysAllowBosses").setValue(true);
                configLoader.save(rootNode);
            }
            else
            {
                rootNode = configLoader.load();
                maxOnServer = rootNode.getNode("Limits", "OnServer").getInt(200);
                maxPerArea = rootNode.getNode("Limits", "PerArea").getInt(60);
                maxPerPlayer = rootNode.getNode("Limits", "PerPlayer").getInt(20);
                maxFlyingPercentage =  rootNode.getNode("Limits", "maxFlyingPercentage").getDouble(0.10);
                maxWaterPercentage = rootNode.getNode("Limits", "maxWaterPercentage").getDouble(0.30);
                alwaysLegendaries = rootNode.getNode("Limits", "alwaysAllowLegendaries").getBoolean(true);
                alwaysBosses = rootNode.getNode("Limits", "alwaysAllowBosses").getBoolean(true);
                
                actualMaxPerPlayer = maxPerPlayer;
                
            } 
        } catch(IOException e) {
            log.error("Failed to create/load or save config" + e.getMessage());
        }
        
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @Listener
    public void onServerStart(GameStartedServerEvent event) 
    {
        spawnRadius = chunkSpawnRadius;
        maxOnlinePlayers = Sponge.getServer().getMaxPlayers();
        maxSpawnsPerTick = 0;
        
        w = Sponge.getServer().getWorlds().toArray(new World[0])[0];
        
        scheduleLog = Sponge.getScheduler().createSyncExecutor(this);
        scheduleLog.scheduleAtFixedRate(new LogStatus(), 60, 60, TimeUnit.SECONDS);
        
        scheduleCleanUp = Sponge.getScheduler().createSyncExecutor(this);
        scheduleCleanUp.scheduleAtFixedRate(new EntitiesAlive(), 500, 500, TimeUnit.MILLISECONDS);
    }
    
    class LogStatus implements Runnable
    {

        @Override
        public void run() 
        {
            log.info("Currently " + spawnedPixelmon.size() + " pokemon spawned. Spawn Per Tick: " + maxSpawnsPerTick);
            
            ArrayList<Player> players = Lists.newArrayList(Sponge.getServer().getOnlinePlayers());
            
            if(!players.isEmpty())
            {
                Player p = players.get(0);
                Tuple t = EntitiesUtility.getNumberOfWildPokemonWithinViewDistanceOfPos(p.getTransform().getPosition(), spawnRadius);
                int wild = (int) t.getFirst();
                int flying = (int)((Tuple)t.getSecond()).getFirst();
                int water = (int)((Tuple)t.getSecond()).getSecond();
                log.info("Found: " + wild + " wild pokemon, " + flying + " air persistent pokemon and " + water + " water pokemon around player " + p.getName());
            }
        }
    }
    
    class EntitiesAlive implements Runnable
    {
        int k = 0;
        @Override
        public void run() 
        {
            ArrayList<UUID> tempList = Lists.newArrayList(spawnedPixelmon);
            
            //Divide work by only looking at 25 entities at a time.
            int iterations = (int)Math.max(25.0, Math.ceil((double)maxOnServer / 25.0));
            int p = (iterations-k)*25;
            
            for(int i=p; i < tempList.size(); i++)
            {
                if(i >= tempList.size())
                {
                    break;
                }
                Optional<Entity> o = w.getEntity(tempList.get(i));
                if(o.isPresent())
                {
                    if(o.get().isRemoved())
                    {
                        spawnedPixelmon.remove(tempList.get(i));
                    }
                } 
                else 
                {
                    spawnedPixelmon.remove(tempList.get(i));
                }
            }
            
            AdjustSpawnRate();
            k++;
            if(k > iterations)
            {
                k=0;
            }
        }
        
    }
    
    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event)
    {
        AdjustSpawnRate();
        log.info("Player joined. Adjusting max spawns per tick to: " + maxSpawnsPerTick);
    }
    
    @Listener
    public void onPlayerLeave(ClientConnectionEvent.Disconnect event)
    {
        AdjustSpawnRate();
        log.info("Player left. Adjusting max spawns per tick to: " + maxSpawnsPerTick);
    }
    
    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event)
    {
        Entity e = (Entity) event.getEntity();
        if(!(e instanceof EntityPixelmon))
        {
            return;
        }
        
        EntityPixelmon poke = (EntityPixelmon)e;
        
        if(poke.forceSpawn)
        {
            return;
        }
        
        if(poke.hasOwner() || poke.hasNPCTrainer)
        {
            return;
        }

        if(alwaysLegendaries)
        {
            if(poke.getSpawnLocation() == SpawnLocation.Legendary)
            {
                return;
            }
        }

        if(alwaysBosses)
        {
            if(poke.getSpawnLocation() == SpawnLocation.Boss)
            {
                return;
            }
        }

        if(spawnedPixelmon.size() >= maxOnServer)
        {
            event.setCanceled(true);
            return;
        }
        //Distance between a player and possible spawn will be the radius * chunkSize.
        ArrayList<Player> nearbyPlayers = EntitiesUtility.getNearbyPlayers(e.getTransform().getPosition(), spawnRadius);
        if(nearbyPlayers.isEmpty())
        {
            event.setCanceled(true);
            return;
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
        double addToViewDistance = Math.sqrt(EntitiesUtility.getEuclidianDistanceSqrd(minPos, maxPos))/16.0;
        avg_pos = avg_pos.div((double)nearbyPlayers.size());

        Tuple t = EntitiesUtility.getNumberOfWildPokemonWithinViewDistanceOfPos(avg_pos, (int)Math.ceil(spawnRadius + addToViewDistance));
        int wildInAreaAroundPlayers = (int) t.getFirst();
        int flyingInAreaAroundPlayers = (int)((Tuple)t.getSecond()).getFirst();
        int waterInAreaAroundPlayers = (int)((Tuple)t.getSecond()).getSecond();

        if(poke.getSpawnLocation() == SpawnLocation.AirPersistent)
        {
            if(flyingInAreaAroundPlayers >= actualMaxPerPlayer*nearbyPlayers.size()*maxFlyingPercentage){
                event.setCanceled(true);
                return;
            } 
        } 
        else if(poke.getSpawnLocation() == SpawnLocation.Water)
        {
            if(waterInAreaAroundPlayers >= actualMaxPerPlayer*nearbyPlayers.size()*maxWaterPercentage){
                event.setCanceled(true);
                return;
            }
        }

        if(wildInAreaAroundPlayers >= maxPerArea)
        {
            event.setCanceled(true);
        } 
        else if(wildInAreaAroundPlayers >= actualMaxPerPlayer*nearbyPlayers.size())
        {
            event.setCanceled(true);
        } 
        else
        {
            spawnedPixelmon(poke);
        }
    }
    
    @Listener
    public void onServerStopped(GameStoppingServerEvent event)
    {
        scheduleLog.shutdown();
        scheduleCleanUp.shutdown();
    }
    
    void spawnedPixelmon(EntityPixelmon p){
        spawnedPixelmon.add(((Entity) p).getUniqueId());
        AdjustSpawnRate();
    }
    
    void AdjustSpawnRate()
    {
        int onlinePlayers = Sponge.getServer().getOnlinePlayers().size();
        if(maxPerPlayer*onlinePlayers > maxOnServer)
        {
            actualMaxPerPlayer = (int)Math.floor((double)maxOnServer / (double)onlinePlayers);
        }
        
        maxSpawnsPerTick = (int)Math.max(1, (onlinePlayers*actualMaxPerPlayer)-spawnedPixelmon.size());
    }
    
}
