package me.remag501.customarmorsets.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Set;

public class LookEntitiesUtil {

    public static LivingEntity getNearestEntityInSight(Player player, int range) {
        ArrayList<Entity> entities = (ArrayList<Entity>) player.getNearbyEntities(range, range, range);
        ArrayList<LivingEntity> livingEntities = new ArrayList<>();
        for (Entity entity: entities) {
            if (entity instanceof LivingEntity)
                livingEntities.add((LivingEntity) entity);
        }
        ArrayList<Block> sightBlock = (ArrayList<Block>) player.getLineOfSight( (Set<Material>) null, range);
        ArrayList<Location> sight = new ArrayList<Location>();
        for (int i = 0;i<sightBlock.size();i++)
            sight.add(sightBlock.get(i).getLocation());
        for (int i = 0;i<sight.size();i++) {
            for (int k = 0;k<livingEntities.size();k++) {
                if (Math.abs(livingEntities.get(k).getLocation().getX()-sight.get(i).getX())<1.3) {
                    if (Math.abs(livingEntities.get(k).getLocation().getY()-sight.get(i).getY())<1.5) {
                        if (Math.abs(livingEntities.get(k).getLocation().getZ()-sight.get(i).getZ())<1.3) {
                            return livingEntities.get(k);
                        }
                    }
                }
            }
        }
        return null; //Return null/nothing if no entity was found
    }
}
