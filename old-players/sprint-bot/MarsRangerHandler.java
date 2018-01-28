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
        MarsController earthParent = (MarsController)parent;
        PlanetMap map = earthParent.map;
        Team enemyTeam = earthParent.enemyTeam;
        TargetingMaster tm = earthParent.tm;        
        PathMaster pm = earthParent.pm;
        // Queue<Integer> attackQueue = earthParent.attackQueue;
        
        // VecUnit nearby = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), gc.team());
        // ArrayList<Unit> nearbyAttackers = new ArrayList<Unit>();
        // ArrayList<Unit> nearbyPassive = new ArrayList<Unit>();
        // load(nearby, nearbyAttackers, nearbyPassive);

        VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(mapLocation, unit.attackRange(), earthParent.enemyTeam);        
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
                if (tm.initial.contains(target.toJson()) && pm.getPathFieldWithCache(target).isPointSet(mapLocation)) {
                    Utils.tryMoveRotate(gc, id, getRandomDirection(mapLocation, target, pm));
                } else {
                    bug.bugMove(mapLocation, target);
                    // Utils.tryMoveRotate(gc, id, mapLocation.directionTo(target));   
                }                
            } else {
                MapLocation enemyLocation = focusEnemy.location().mapLocation();
                if (mapLocation.distanceSquaredTo(enemyLocation) < 42) {
                    if (gc.isAttackReady(id) && gc.canAttack(id, focusEnemy.id())) {
                        gc.attack(id, focusEnemy.id());
                    }
                    Utils.tryMoveWiggleRecur(gc, id, enemyLocation.directionTo(mapLocation), null);
                } else {                
                    bug.bugMove(mapLocation, target);
                    // Utils.tryMoveRotate(gc, id, mapLocation.directionTo(target));
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
                bug.bugMove(mapLocation, target);
                // Utils.tryMoveRotate(gc, id, mapLocation.directionTo(target));
                if (gc.isAttackReady(id) && gc.canAttack(id, focusEnemy.id())) {
                    gc.attack(id, focusEnemy.id());
                }
            }                
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
}