import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.Queue;

public class HealerHandler extends UnitHandler {

    public HealerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }

    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        
    
    @Override
    public void takeTurn(Unit unit) {        

        Location location = unit.location();

        if (!location.isOnMap()) {            
            return;
        }

        MapLocation mapLocation = location.mapLocation();

        if (location.isOnPlanet(Planet.Mars)) {
            System.out.println("LITTINGTON_BILLINGTON");
            return;
        }

        // references to parent
        EarthController earthParent = (EarthController)parent;
        PlanetMap map = earthParent.map;
        Team enemyTeam = earthParent.enemyTeam;
        TargetingMaster tm = earthParent.tm;        
        PathMaster pm = earthParent.pm;
        // Queue<Integer> attackTargets = earthParent.attackTargets;
        
    	VecUnit nearby = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), gc.team()); //only those that i can help... 
        ArrayList<Unit> nearbyAttackers = new ArrayList<Unit>();
        ArrayList<Unit> nearbyPassive = new ArrayList<Unit>();
        load(nearby, nearbyAttackers, nearbyPassive);        
        
        if(gc.isAttackReady(this.id)) { //heal nearest unit    
            Unit healTarget = getMostHurt(nearbyAttackers, unit.attackRange());
            
        	if(healTarget != null && gc.canHeal(this.id, healTarget.id())) {
        		gc.heal(this.id, healTarget.id());
            }
        }
        
        if(gc.isMoveReady(this.id)) {
        	Unit moveTarget = getMostHurt(nearbyAttackers, unit.visionRange());
        	Direction moveDir = moveTarget != null ? mapLocation.directionTo(moveTarget.location().mapLocation()) : null;
        	if(moveDir != null) {
        		Utils.tryMoveRotate(gc, this.id, moveDir);
        	}
        	else {
        		MapLocation target = getTarget(mapLocation, unit.visionRange(), enemyTeam, tm);
        		
        		if(target != null) Utils.tryMoveRotate(gc, this.id, mapLocation.directionTo(target));
        	}
        }
        
        

        MapLocation target = getTarget(mapLocation, unit.visionRange(), enemyTeam, tm);

        Utils.tryMoveRotate(gc, id, mapLocation.directionTo(target));
    }

    public Direction getRandomDirection(MapLocation mapLocation, MapLocation targetLocation, PathMaster pm) {
        Direction[] dirs = pm.getPathFieldWithCache(targetLocation).getDirectionsAtPoint(mapLocation);
        return dirs[rng.nextInt(dirs.length)];
    }

    private void load(VecUnit all, ArrayList<Unit> attackers, ArrayList<Unit> passive) {
        for (int i = 0; i < all.size(); i++) {
            Unit unit = all.get(i);
            UnitType type = unit.unitType();
            if (type != UnitType.Worker && type != UnitType.Rocket && type != UnitType.Factory) {
                attackers.add(unit);
            } else {
                passive.add(unit);
            }
        }
    }

    private Unit getClosestEnemy(ArrayList<Unit> nearbyEnemies, MapLocation mapLocation) {
        Unit closestEnemy = null;
        long closestDistance = Long.MAX_VALUE;
        for (int i = 0; i < nearbyEnemies.size(); i++) {            
            Unit nearbyUnit = nearbyEnemies.get(i);
            MapLocation tryLocation = nearbyUnit.location().mapLocation();
            long distance = mapLocation.distanceSquaredTo(tryLocation);
            if (distance < closestDistance) {
                closestEnemy = nearbyUnit;
                closestDistance = distance;
            }
        }
        return closestEnemy;
    }
    
    private Unit getMostHurt(ArrayList<Unit> friends, long range) {
    	long minHealth = Long.MAX_VALUE;
    	Unit ret = null;
    	MapLocation myLocation = gc.unit(this.id).location().mapLocation();
    	for(Unit friend : friends) {
    		if(myLocation.distanceSquaredTo(friend.location().mapLocation()) > range) continue;
    		long health = friend.health();
    		long maxHealth = friend.maxHealth();
    		if(health < maxHealth && health < minHealth) { //hurt and most hurt
    			minHealth = health;
    			ret = friend;
    		}
    	}
    	return ret;
    }

    private MapLocation getTarget(MapLocation requestLocation, long range, Team enemyTeam, TargetingMaster tm) {
        while (true) {
            MapLocation target = tm.getTarget(requestLocation);
            if (target == null) {
                break;
            }
            if (target.isWithinRange(range, requestLocation) && gc.senseNearbyUnitsByTeam(target, range, enemyTeam).size() == 0) {
                tm.removeTarget(target);
                continue;
            }
            return target;
        }        
        return null;
    }
}