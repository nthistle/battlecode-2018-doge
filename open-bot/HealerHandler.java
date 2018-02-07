import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.Queue;
import java.util.Collections;

public class HealerHandler extends UnitHandler {

    private Bug bug;
    private EarthController earthParent;    

	public Team enemy; 
	public MapLocation myLocation;
	public Map<Direction, Integer> threatMap;

    public HealerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        earthParent = (EarthController)parent;
        bug = new Bug(gc, id, earthParent.map);
        enemy = earthParent.enemyTeam;
        myLocation = gc.unit(id).location().mapLocation();
        threatMap = new EnumMap<Direction, Integer>(Direction.class);
        for(Direction dir : Direction.values()) {
        	threatMap.put(dir, 0);
        }        
    }

    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        
    
    @Override
    public void takeTurn(Unit unit) {        

        Location location = unit.location();

        if (!location.isOnMap() || location.isInGarrison() || location.isInSpace()) {            
            System.out.println("HEH!");
			return;
        }


        if (location.isOnPlanet(Planet.Mars)) {            
            return;
        }
        
        myLocation = unit.location().mapLocation();

        // references to parent        
        PlanetMap map = earthParent.map;        
        TargetingMaster tm = earthParent.tm;        
        PathMaster pm = earthParent.pm;
        // Queue<Integer> attackTargets = earthParent.attackTargets;
        
    	VecUnit nearby = gc.senseNearbyUnitsByTeam(myLocation, unit.visionRange(), gc.team()); //only those that i can help... 
        ArrayList<Unit> nearbyAttackers = new ArrayList<Unit>();
        ArrayList<Unit> nearbyPassive = new ArrayList<Unit>();
        load(nearby, nearbyAttackers, nearbyPassive);        
        
        // must heal before we move
        if(gc.isHealReady(this.id)) { //heal nearest unit    
            Unit healTarget = getMostHurt(unit);
        	if(healTarget != null && gc.canHeal(this.id, healTarget.id())) {
        		gc.heal(this.id, healTarget.id());
            }
        }
        //System.out.println(myLocation);
        if(gc.isMoveReady(this.id)) {        
        	VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(myLocation, 100, enemy);
        	//System.out.println(nearby.size());
        	//System.out.println(nearbyEnemies.size());
        	//System.out.println(enemy);
        	Direction runAwayDir = getRunAwayDirection(nearbyEnemies);
        	//System.out.println(this.id + ": " + runAwayDir);
        	if(runAwayDir != Direction.Center) 
        		Utils.tryMoveWiggle(gc, id, runAwayDir);
        	else {
        	    Unit moveTarget = getMostHurt(unit);                        	
        		if(moveTarget != null) {
                    MapLocation target = moveTarget.location().mapLocation();        			
                    if (pm.isCached(target) && pm.getPathFieldWithCache(target).isPointSet(myLocation)) {
                        Utils.tryMoveRotate(gc, id, getRandomDirection(myLocation, target, pm));
                    } else {
                        bug.bugMove(myLocation, target);                            
                    }
        		}
        		else {
        			MapLocation target = getTarget(myLocation, unit.visionRange(), enemy, tm);
        			if(target != null) 
                        if (pm.isCached(target) && pm.getPathFieldWithCache(target).isPointSet(myLocation)) {
                            Utils.tryMoveRotate(gc, id, getRandomDirection(myLocation, target, pm));
                        } else {
                            bug.bugMove(myLocation, target);                            
                        }                
        		}
        	}
        }

        if (!gc.isMoveReady(id)) {
            return;
        }

        for(int i = 0; i < 5; i ++) {
            Direction moveDir = Utils.getRandomDirection(Direction.values(), this.rng);
            if(gc.canMove(this.id, moveDir)) {
                gc.moveRobot(this.id, moveDir);
                break;
            }
        }
        //System.out.println(unit.location().mapLocation());
    }

    public Direction getRandomDirection(MapLocation mapLocation, MapLocation targetLocation, PathMaster pm) {
        Direction[] dirs = pm.getPathFieldWithCache(targetLocation).getDirectionsAtPoint(mapLocation);
        return dirs[rng.nextInt(dirs.length)];
    }
    
    public Direction getRunAwayDirection(VecUnit threats) {
    	for(Direction dir : threatMap.keySet()) {
    		threatMap.put(dir, 0);
    	}
    	for(int i = 0; i < threats.size(); i++) {
    		Unit bih = threats.get(i);
    		//System.out.println(bih);
    		MapLocation bihLoc = bih.location().mapLocation();
    		UnitType bihType = bih.unitType();
    		int dist = (int)myLocation.distanceSquaredTo(bihLoc);
    		Direction dir = bihLoc.directionTo(myLocation);
    		if(bihType == UnitType.Ranger) {
    			if(dist <= 80) {
    				threatMap.put(dir, threatMap.get(dir) + 30);
    			}
    		}
    		else if(bihType == UnitType.Knight) {
    			if(dist <= 32) {
    				threatMap.put(dir, threatMap.get(dir) + 80);
    			}
    		}
    		else if(bihType == UnitType.Mage) {
    			if(dist <= 60) {
    				threatMap.put(dir, threatMap.get(dir) + 105); 
    			}
    		}
    		else if(bihType == UnitType.Healer || bihType == UnitType.Worker) {
    			if(dist <= 80) {
    				threatMap.put(dir, threatMap.get(dir) + 30); //i mean, we need to run away from enemy healers & workers, just not very fast
    			}
    		}
    		else if(bihType == UnitType.Rocket || bihType == UnitType.Rocket) {
    			if(dist <= 100) {
    				threatMap.put(dir, threatMap.get(dir) + 5 * (50 - dist)); //weigh danger by distance past 30 as f(dist) = 5 * (50 - dist) 
    			}
    		}
    		else { //no immediate danger, weight 10
    			threatMap.put(dir,  threatMap.get(dir) + 10);
    		}
    	}
    	//System.out.println(threatMap);
    	Direction ret = Direction.Center;
    	int maxIncomingDmg = 0;
    	for(Direction dir : threatMap.keySet()) {
    		if(threatMap.get(dir) > maxIncomingDmg) {
    			maxIncomingDmg = threatMap.get(dir);
    			ret = dir;
    		}
    	}
    	return ret; 
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
    
    private Unit getMostHurt(Unit unit) {
    	long minHealth = Long.MAX_VALUE;
    	Unit ret = null;
    	MapLocation myLocation = unit.location().mapLocation();
        VecUnit canHealFriends = gc.senseNearbyUnitsByTeam(myLocation, unit.attackRange(), gc.team());
        for(int i = 0; i < canHealFriends.size(); i ++) {
    	// for(Unit friend : canHealFriends) {
            Unit friend = canHealFriends.get(i);
    		// if(myLocation.distanceSquaredTo(friend.location().mapLocation()) > range) continue;
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

    public void handleDeath() {}
}
