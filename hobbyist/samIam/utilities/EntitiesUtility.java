package hobbyist.samIam.utilities;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.database.SpawnLocation;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import hobbyist.samIam.dynamicspawn.DynamicSpawn;
import java.util.ArrayList;
import java.util.Optional;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.Chunk;

public class EntitiesUtility {
    
    public static ArrayList<EntityPixelmon> getWildPokemonWithinViewDistanceOfPos(Vector3d pos, int ViewDistance)
    {
        ArrayList<EntityPixelmon> entities = new ArrayList<>();
        
        ArrayList<Entity> allEntities = getEntitiesFromNearbyChunks(pos, ViewDistance);
        
        for(Entity e : allEntities)
        {
            if(e instanceof EntityPixelmon)
            {
                if(((EntityPixelmon) e).getOwner() == null)
                {
                    if(isWithinEuclidianDistance(pos, e.getTransform().getPosition(), ViewDistance*16.0))
                    {
                        entities.add((EntityPixelmon)e);
                    }
                }
            }
        }
        return entities;
    }
    
    //** Returns a tuple of <Total Wild Pokemon, Air Persistent>
    public static Tuple getNumberOfWildPokemonWithinViewDistanceOfPos(Vector3d pos, int ViewDistance)
    {
        int wild = 0;
        int air_persistent = 0;
        int water = 0;
        ArrayList<Entity> allEntities = getEntitiesFromNearbyChunks(pos, ViewDistance);
        
        for(Entity e : allEntities)
        {
            if(e instanceof EntityPixelmon)
            {
                if(((EntityPixelmon) e).getOwner() == null)
                {
                    if(isWithinEuclidianDistance(pos, e.getTransform().getPosition(), ViewDistance*16.0))
                    {
                        wild++;
                        if(((EntityPixelmon) e).getSpawnLocation() == SpawnLocation.AirPersistent){
                           air_persistent++; 
                        } 
                        else if(((EntityPixelmon) e).getSpawnLocation() == SpawnLocation.Water)
                        {
                            water++;
                        }
                    }
                }
            }
        }
        
        Tuple types = Tuple.of(air_persistent, water);
        Tuple t = Tuple.of(wild , types);
        return t;
    }
    
    public static ArrayList<Player> getNearbyPlayers(Vector3d pos, double withinChunks)
    {
        ArrayList<Player> players = new ArrayList<>();
        
        ArrayList<Player> allPlayers = Lists.newArrayList(DynamicSpawn.instance.w.getPlayers());

        for(Player p : allPlayers)
        {
            if(isWithinEuclidianDistance(pos, p.getTransform().getPosition(), withinChunks*16.0))
            {
                players.add(p);
            }
        }
        return players ;
    }
    
    public static boolean isWithinEuclidianDistance(Vector3d p1, Vector3d p2, double limit)
    {
        return (getEuclidianDistanceSqrd(p1, p2) <= (limit*limit));
    }
    
    public static double getEuclidianDistanceSqrd(Vector3d p1, Vector3d p2)
    {
        return (Math.pow(Math.abs(p1.getX() - p2.getX()),2.0) + Math.pow(Math.abs(p1.getZ() - p2.getZ()),2.0));
    }
    
    public static ArrayList<Entity> getEntitiesFromNearbyChunks(Vector3d pos, int distanceInChunks)
    {
        ArrayList<Entity> entities = new ArrayList<>(); 
        Chunk center = DynamicSpawn.instance.w.getChunkAtBlock(pos.toInt()).get();
        for(int x=-distanceInChunks; x<=distanceInChunks; x++)
        {
            for(int z=-distanceInChunks; z<=distanceInChunks; z++)
            {
                Optional<Chunk> chunk = DynamicSpawn.instance.w.getChunk(center.getPosition().getX()+x, center.getPosition().getY(), center.getPosition().getZ()+z);
                if(chunk.isPresent())
                {
                    Chunk c = chunk.get();
                    entities.addAll(c.getEntities());
                }
            }
        }
        
        return entities;
    }
    
}
