package hobbyist.samIam.utilities;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.database.SpawnLocation;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import hobbyist.samIam.dynamicspawn.DynamicSpawn;
import java.util.ArrayList;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Tuple;

public class EntitiesUtility {
    
    public static ArrayList<EntityPixelmon> getWildPokemonWithinViewDistanceOfPos(Vector3d pos, double ViewDistance)
    {
        ArrayList<EntityPixelmon> entities = new ArrayList<>();
        
        ArrayList<Entity> allEntities = Lists.newArrayList(DynamicSpawn.instance.w.getEntities());
        
        for(Entity e : allEntities)
        {
            if(e instanceof EntityPixelmon)
            {
                if(((EntityPixelmon) e).getOwner() == null)
                {
                    if(getEuclidianDistance(pos, e.getTransform().getPosition()) <= 16.0*ViewDistance)
                    {
                        entities.add((EntityPixelmon)e);
                    }
                }
            }
        }
        return entities;
    }
    
    //** Returns a tuple of <Total Wild Pokemon, Air Persistent>
    public static Tuple getNumberOfWildPokemonWithinViewDistanceOfPos(Vector3d pos, double ViewDistance)
    {
        int wild = 0;
        int air_persistent = 0;
        int water = 0;
        ArrayList<Entity> allEntities = Lists.newArrayList(DynamicSpawn.instance.w.getEntities());
        
        for(Entity e : allEntities)
        {
            if(e instanceof EntityPixelmon)
            {
                if(((EntityPixelmon) e).getOwner() == null)
                {
                    if(getEuclidianDistance(pos, e.getTransform().getPosition()) <= (16.0*ViewDistance))
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
    
    public static ArrayList<Player> getNearbyPlayers(Entity e, double withinChunks)
    {
        ArrayList<Player> players = new ArrayList<>();
        
        ArrayList<Player> allPlayers = Lists.newArrayList(DynamicSpawn.instance.w.getPlayers());

        for(Player p : allPlayers)
        {
            if(getEuclidianDistance(e.getTransform().getPosition(), p.getTransform().getPosition()) <= withinChunks*16.0)
            {
                players.add(p);
            }
        }
        return players ;
    }
    
    
    public static double getEuclidianDistance(Vector3d p1, Vector3d p2){
        double dist = Math.sqrt(Math.pow(Math.abs(p1.getX() - p2.getX()),2.0) + Math.pow(Math.abs(p1.getZ() - p2.getZ()),2.0));
        return dist;
    }
    
}
