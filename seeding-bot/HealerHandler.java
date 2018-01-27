import bc.*;
import java.util.*;

public class HealerHandler extends UnitHandler {
	public Team enemy; 
	public MapLocation myLocation;
	public Map<Direction, Integer> threatMap;
    public HealerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        enemy = ((EarthController)parent).enemyTeam;
        myLocation = gc.unit(id).location().mapLocation();
        threatMap = new EnumMap<Direction, Integer>(Direction.class);
        for(Direction dir : Direction.values()) {
        	if(dir == Direction.Center) continue;
        	else {
        		threatMap.put(dir, 0);
        	}
        }
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


        if (location.isOnPlanet(Planet.Mars)) {
            //System.out.println("LITTINGTON_BILLINGTON");
            return;
        }

        // references to parent
        EarthController earthParent = (EarthController)parent;
        PlanetMap map = earthParent.map;
        Team enemyTeam = earthParent.enemyTeam;
        TargetingMaster tm = earthParent.tm;        
        PathMaster pm = earthParent.pm;
        // Queue<Integer> attackTargets = earthParent.attackTargets;
        
    	VecUnit nearby = gc.senseNearbyUnitsByTeam(myLocation, unit.visionRange(), gc.team()); //only those that i can help... 
        ArrayList<Unit> nearbyAttackers = new ArrayList<Unit>();
        ArrayList<Unit> nearbyPassive = new ArrayList<Unit>();
        load(nearby, nearbyAttackers, nearbyPassive);        
        
        if(gc.isHealReady(this.id)) { //heal nearest unit    
            Unit healTarget = getMostHurt(nearbyAttackers, unit.attackRange());
        	if(healTarget != null && gc.canHeal(this.id, healTarget.id())) {
        		gc.heal(this.id, healTarget.id());
            }
        }
        System.out.println(myLocation);
        if(gc.isMoveReady(this.id)) {        	
        	Direction runAwayDir = getRunAwayDirection(gc.senseNearbyUnitsByTeam(myLocation, 50, enemy));
        	System.out.println(this.id + ": " + runAwayDir);
        	if(runAwayDir != Direction.Center) 
        		Utils.tryMoveWiggleRecur(gc, id, runAwayDir, null);
        	else {
        		Unit moveTarget = getMostHurt(nearbyAttackers, unit.visionRange());
        		Direction moveDir = moveTarget != null ? myLocation.directionTo(moveTarget.location().mapLocation()) : null;
        		if(moveDir != null) {
        			Utils.tryMoveWiggleRecur(gc, this.id, moveDir, null);
        		}
        		else {
        			MapLocation target = getTarget(myLocation, unit.visionRange(), enemyTeam, tm);
        			if(target != null) Utils.tryMoveRotate(gc, this.id, myLocation.directionTo(target));
        		}
        	}
        }
        System.out.println(unit.location().mapLocation());
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
    		MapLocation bihLoc = bih.location().mapLocation();
    		UnitType bihType = bih.unitType();
    		int dist = (int)myLocation.distanceSquaredTo(bihLoc);
    		Direction dir = bihLoc.directionTo(myLocation);
    		if(bihType == UnitType.Ranger) {
    			if(dist >= 10 && dist <= 50) {
    				threatMap.put(dir, threatMap.get(dir) + 30);
    			}
    		}
    		else if(bihType == UnitType.Knight) {
    			if(dist <= 2) {
    				threatMap.put(dir, threatMap.get(dir) + 80);
    			}
    		}
    		else if(bihType == UnitType.Mage) {
    			if(dist <= 30) {
    				threatMap.put(dir, threatMap.get(dir) + 105); 
    			}
    		}
    		else if(bihType == UnitType.Healer || bihType == UnitType.Worker) {
    			if(dist <= 50) {
    				threatMap.put(dir, threatMap.get(dir) + 30); //i mean, we need to run away from enemy healers & workers, just not very fast
    			}
    		}
    		else if(bihType == UnitType.Rocket || bihType == UnitType.Rocket) {
    			if(dist <= 50) {
    				threatMap.put(dir, threatMap.get(dir) + 5 * (50 - dist)); //weigh danger by distance past 30 as f(dist) = 5 * (50 - dist) 
    			}
    		}
    	}
    	System.out.println(threatMap);
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

    public void handleDeath() {}
}
