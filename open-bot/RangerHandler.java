import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

public class RangerHandler extends UnitHandler {

    private Bug bug;
    private EarthController earthParent;

    public RangerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        earthParent = (EarthController)parent;
        bug = new Bug(gc, id, earthParent.map);
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
            return;
        }

        // references to parent        
        PlanetMap map = earthParent.map;        
        PathMaster pm = earthParent.pm;

        VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(mapLocation, unit.attackRange(), earthParent.enemyTeam);        
        ArrayList<Unit> nearbyEnemyAttackers = new ArrayList<Unit>();
        ArrayList<Unit> nearbyEnemyPassive = new ArrayList<Unit>();
        load(nearbyEnemies, nearbyEnemyAttackers, nearbyEnemyPassive);        

        MapLocation target = getTarget(mapLocation, (int)unit.visionRange());
                
        Unit focusEnemy = getClosestEnemy(nearbyEnemyAttackers, mapLocation);
        if (focusEnemy != null && focusEnemy.location().mapLocation().distanceSquaredTo(mapLocation) > 12) {
            focusEnemy = getWeakestEnemy(nearbyEnemyAttackers, mapLocation);
        }
        if (focusEnemy == null) {
            focusEnemy = getClosestEnemy(nearbyEnemyPassive, mapLocation);
        }

        if (target != null) {
            if (focusEnemy == null) {
                if (pm.isCached(target) && pm.getPathFieldWithCache(target).isPointSet(mapLocation)) {
                    Utils.tryMoveRotate(gc, id, getRandomDirection(mapLocation, target, pm));
                } else {
                    bug.bugMove(mapLocation, target);                    
                }                
            } else {
                MapLocation enemyLocation = focusEnemy.location().mapLocation();
                if (mapLocation.distanceSquaredTo(enemyLocation) < 42) {
                    if (gc.isAttackReady(id) && gc.canAttack(id, focusEnemy.id())) {
                        gc.attack(id, focusEnemy.id());
                    }                    
                    Utils.tryMoveWiggle(gc, id, enemyLocation.directionTo(mapLocation));
                } else {                
                    bug.bugMove(mapLocation, target);                    
                    if (gc.isAttackReady(id) && gc.canAttack(id, focusEnemy.id())) {
                        gc.attack(id, focusEnemy.id());
                    }
                }                
            }
        } else if (focusEnemy != null) {
            MapLocation enemyLocation = focusEnemy.location().mapLocation();
            if (mapLocation.distanceSquaredTo(enemyLocation) < 42) {
                if (gc.isAttackReady(id) && gc.canAttack(id, focusEnemy.id())) {
                    gc.attack(id, focusEnemy.id());
                }                
                Utils.tryMoveWiggle(gc, id, enemyLocation.directionTo(mapLocation));
            } else {                
                bug.bugMove(mapLocation, enemyLocation);                                
                if (gc.isAttackReady(id) && gc.canAttack(id, focusEnemy.id())) {
                    gc.attack(id, focusEnemy.id());
                }
            }                
        }

        if (!gc.isMoveReady(id)) {
            return;
        }

        for (int i = 0; i < 8; i++) {
            Direction moveDir = Utils.getRandomDirection(Direction.values(), this.rng);
            if (gc.canMove(this.id, moveDir)) {
                gc.moveRobot(this.id, moveDir);
                break;
            }
        }
    }

    public Direction getRandomDirection(MapLocation mapLocation, MapLocation targetLocation, PathMaster pm) {
        PathField pf = pm.getPathFieldWithCache(targetLocation);
        PathField.PathPoint pp = pf.getPoint(mapLocation);
        return pp.dirs[rng.nextInt(pp.numDirs)];
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

    private Unit getWeakestEnemy(ArrayList<Unit> nearbyEnemies, MapLocation mapLocation) {
        Unit weakestEnemy = null;
        int lowestHealth = Integer.MAX_VALUE;
        for (int i = 0; i < nearbyEnemies.size(); i++) {            
            Unit nearbyUnit = nearbyEnemies.get(i);            
            MapLocation tryLocation = nearbyUnit.location().mapLocation();
            int health = (int)nearbyUnit.health();
            int distance = (int)mapLocation.distanceSquaredTo(tryLocation);
            if (distance > 10 && health < lowestHealth) {
                weakestEnemy = nearbyUnit;
                lowestHealth = health;
            }
        }
        return weakestEnemy;
    }


    private Unit getClosestEnemy(ArrayList<Unit> nearbyEnemies, MapLocation mapLocation) {
        Unit closestEnemy = null;
        int closestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < nearbyEnemies.size(); i++) {            
            Unit nearbyUnit = nearbyEnemies.get(i);
            MapLocation tryLocation = nearbyUnit.location().mapLocation();
            int distance = (int)mapLocation.distanceSquaredTo(tryLocation);
            if (distance < closestDistance) {
                closestEnemy = nearbyUnit;
                closestDistance = distance;
            }
        }
        return closestEnemy;
    }

    private MapLocation getTarget(MapLocation requestLocation, int range) {
        TargetingMaster tm = earthParent.tm;
        Team enemyTeam = earthParent.enemyTeam;
        while (true) {
            MapLocation target = tm.getTarget(requestLocation);
            if (target == null) {
                break;
            }
            if (((target.isWithinRange(range - 2, requestLocation) && gc.senseNearbyUnitsByTeam(target, range, enemyTeam).size() == 0) || target.isWithinRange(2, requestLocation)) && tm.targets.size() > 1) {                
                tm.removeTarget(target);
                continue;
            }
            return target;
        }        
        return null;
    }

    public void handleDeath() {}
}