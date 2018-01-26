import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

public class MarsRangerHandler extends UnitHandler {

    private Bug bug;    

    public MarsRangerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        bug = new Bug(gc, id, ((MarsController)parent).map);
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
        Location location = unit.location();

        if (!location.isOnMap()) {            
            return;
        }

        MapLocation mapLocation = location.mapLocation();

        if (location.isOnPlanet(Planet.Earth)) {           
            return;
        }

        // references to parent
        MarsController marsPlanet = (MarsController)parent;
        PlanetMap map = marsPlanet.map;
        Team enemyTeam = marsPlanet.enemyTeam;
        TargetingMaster tm = marsPlanet.tm;        
        PathMaster pm = marsPlanet.pm;
        // Queue<Integer> attackQueue = marsPlanet.attackQueue;
        
        // VecUnit nearby = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), gc.team());
        // ArrayList<Unit> nearbyAttackers = new ArrayList<Unit>();
        // ArrayList<Unit> nearbyPassive = new ArrayList<Unit>();
        // load(nearby, nearbyAttackers, nearbyPassive);

        VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(mapLocation, unit.attackRange(), enemyTeam);        
        ArrayList<Unit> nearbyEnemyAttackers = new ArrayList<Unit>();
        ArrayList<Unit> nearbyEnemyPassive = new ArrayList<Unit>();
        load(nearbyEnemies, nearbyEnemyAttackers, nearbyEnemyPassive);        

        MapLocation target = getTarget(mapLocation, unit.visionRange(), enemyTeam, tm);

        Unit focusEnemy = getClosestEnemy(nearbyEnemyAttackers, mapLocation);
        if (focusEnemy != null && focusEnemy.location().mapLocation().distanceSquaredTo(mapLocation) > 12) {
            focusEnemy = getWeakestEnemy(nearbyEnemyAttackers, mapLocation);
        }
        if (focusEnemy == null) {
            focusEnemy = getClosestEnemy(nearbyEnemyPassive, mapLocation);
        }

        if (target != null) {
            if (focusEnemy == null) {                
                bug.bugMove(mapLocation, target);                                    
            } else {
                MapLocation enemyLocation = focusEnemy.location().mapLocation();
                if (mapLocation.distanceSquaredTo(enemyLocation) < 42) {
                    if (gc.isAttackReady(id) && gc.canAttack(id, focusEnemy.id())) {
                        gc.attack(id, focusEnemy.id());
                    }
                    Utils.tryMoveWiggleRecur(gc, id, enemyLocation.directionTo(mapLocation), null);
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
                Utils.tryMoveWiggleRecur(gc, id, enemyLocation.directionTo(mapLocation), null);
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

        // random movement
        for(int i = 0; i < 5; i ++) {
            Direction moveDir = Utils.getRandomDirection(Direction.values(), this.rng);
            if(gc.canMove(this.id, moveDir)) {
                gc.moveRobot(this.id, moveDir);
                break;
            }
        }
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
        Unit closestEnemy = null;
        long closestDistance = Long.MAX_VALUE;
        for (int i = 0; i < nearbyEnemies.size(); i++) {            
            Unit nearbyUnit = nearbyEnemies.get(i);            
            long distance = nearbyUnit.health();
            if (distance < closestDistance) {
                closestEnemy = nearbyUnit;
                closestDistance = distance;
            }
        }
        return closestEnemy;
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

    private MapLocation getTarget(MapLocation requestLocation, long range, Team enemyTeam, TargetingMaster tm) {
        while (true) {
            MapLocation target = tm.getTarget(requestLocation);
            if (target == null) {
                break;
            }
            if ((target.isWithinRange(range - 2, requestLocation) && gc.senseNearbyUnitsByTeam(target, range, enemyTeam).size() == 0) || target.isWithinRange(2, requestLocation)) && tm.targets.size() > 1) {                                    
                //System.out.println("BIG_BOY" + target);
                tm.removeTarget(target);
                continue;
            }
            return target;
        }        
        return null;
    }
}