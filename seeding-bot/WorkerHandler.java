import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

// TODO:
// smarter factory placement -> adjacencies and avoid putting on money
// rotation around structure while building
// Mars

public class WorkerHandler extends UnitHandler {
    
    private MapLocation previousLocation = null; // temporary feature, will be removed

    public WorkerHandler(PlanetController parent, GameController gc, int id, Random rng) {
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
            return;
        }

        // references to parent 
        EarthController earthParent = (EarthController)parent;
        PlanetMap map = earthParent.map;
        HashMap<UnitType, Integer> robotCount = earthParent.robotCount;        
        
        // status markers        
        boolean busy = false;
        boolean done = false;

        VecUnit nearbyAllies = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), gc.team());
        VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), earthParent.enemyTeam);        
        ArrayList<Unit> nearbyStructures = new ArrayList<Unit>(); // ally only                

        // count certain robots in vision range for ratio purposes
        // store them as well as precompute nearest
        int nearbyBuiltStructureCount = 0;
        int nearbyWorkerCount = 0;
        int nearbyTroopCount = 0;
        PathField structurePath = null;
        MapLocation nearestStructure = null;
        long nearestDistance = Long.MAX_VALUE;
        for (int i = 0; i < nearbyAllies.size(); i++) {
            Unit allyUnit = nearbyAllies.get(i);
            UnitType unitType = allyUnit.unitType();
            if (unitType == UnitType.Worker) {
                nearbyWorkerCount++;
            } else if (unitType == UnitType.Factory || unitType == UnitType.Rocket) {                
                if (allyUnit.structureIsBuilt() == 1) {
                    nearbyBuiltStructureCount++;
                    continue;
                }
                nearbyStructures.add(allyUnit);
                MapLocation tryLocation = allyUnit.location().mapLocation();
                long distance;
                PathField path = null;
                if (gc.round() < 100) {
                    path = earthParent.pm.getPathField(tryLocation);
                    if (!path.isPointSet(mapLocation)) {
                        continue;
                    }
                    distance = path.getDistanceAtPoint(mapLocation);
                } else {
                    distance = mapLocation.distanceSquaredTo(tryLocation);
                }
                if (nearestStructure == null || distance < nearestDistance) {
                    nearestStructure = tryLocation;
                    nearestDistance = distance;
                    if (gc.round() < 100) {
                        structurePath = path;   
                    }                    
                }                    
            } else {
                nearbyTroopCount++;
            }
        }

        // if building commit and replicate until 3 workers are present at site
        if (nearbyStructures.size() > 0 && nearbyWorkerCount < 3) {
            if (nearestStructure != null && Utils.tryReplicateRotate(gc, id, mapLocation.directionTo(nearestStructure))) {
                earthParent.incrementRobotCount(UnitType.Worker);
            } else {
                for (Direction d : Utils.directions()) {
                    if (gc.canReplicate(id, d)) {
                        gc.replicate(id, d);     
                        earthParent.incrementRobotCount(UnitType.Worker);
                        break;
                    }
                }
            }
        }
    
        // potential balancing needed here: stop committing to build if completely outnumbered

        // try to build and commit if possible
        for (int i = 0; i < nearbyStructures.size(); i++) {
            Unit nearbyUnit = nearbyStructures.get(i);            
            if (gc.canBuild(id, nearbyUnit.id())) {
                gc.build(id, nearbyUnit.id());                
                busy = true;                
                done = true;
                break;
            }
        }

        // find nearest enemy
        // if necessary TODO: find nearest enemies plural for best escape route
        int nearbyThreatCount = 0;
        MapLocation nearestThreat = null;
        nearestDistance = Long.MAX_VALUE;
        for (int i = 0; i < nearbyEnemies.size(); i++) {
            nearbyThreatCount++;
            Unit nearbyUnit = nearbyEnemies.get(i);
            UnitType type = nearbyUnit.unitType();
            if (type == UnitType.Factory || type == UnitType.Rocket || type == UnitType.Worker) {
                continue;
            }
            MapLocation tryLocation = nearbyUnit.location().mapLocation();
            long distance = mapLocation.distanceSquaredTo(tryLocation);
            if (distance - 2 <= nearbyUnit.attackRange() && distance < nearestDistance) {
                nearestThreat = tryLocation;
                nearestDistance = distance;
            }
        }

        // if within attack range of enemy
        // don't try to assist in building and run in opposite direction
        if (!busy && nearestThreat != null && robotCount.get(UnitType.Factory) != 0 && Utils.tryMoveRotate(gc, id, nearestThreat.directionTo(mapLocation))) {
            busy = true;            
            previousLocation = mapLocation;
        }

        // if cannot build but there are nearby structures move towards them
        if (!busy && nearestStructure != null) {
            if (gc.round() < 100 && Utils.tryMoveRotate(gc, id, structurePath.getDirectionAtPoint(mapLocation))) {
                busy = true;
                previousLocation = mapLocation;
            } else if (Utils.tryMoveRotate(gc, id, mapLocation.directionTo(nearestStructure))) {
                busy = true;
                previousLocation = mapLocation;
            }
        }

        // simple rocket build code
        // conditions will be decided later
        // rest is handled by rocket handlers
        // if (!busy && (gc.round() > 600 || gc.getTimeLeftMs() < 1000)) {
        //     for (Direction d : Utils.directions()) {
        //         if (gc.canBlueprint(id, UnitType.Rocket, d)) {
        //             gc.blueprint(id, UnitType.Rocket, d);
        //             earthParent.incrementRobotCount(UnitType.Rocket);
        //             busy = true;
        //             done = true;
        //             break;
        //         }                
        //     }        
        //     return;            
        // }        

        // if conditions are appropriate blueprint factory
        long totalStructures = nearbyStructures.size() + nearbyBuiltStructureCount;
        if (!busy && gc.karbonite() >= 100 && (earthParent.getRobotCount(UnitType.Factory) == 0 || (earthParent.getRobotCount(UnitType.Factory) < 3 && nearbyWorkerCount > 1) || (earthParent.getRobotCount(UnitType.Factory) >= 3 && totalStructures == 0) || totalStructures > 0)) {
            Direction buildDirection = findBuildDirection(unit);            
            if (buildDirection != null && gc.canBlueprint(id, UnitType.Factory, buildDirection)) {
                gc.blueprint(id, UnitType.Factory, buildDirection);
                earthParent.incrementRobotCount(UnitType.Factory);
                busy = true;
                done = true;                
            }
        }                

        if (!done) {
            Direction harvestDirection = null;            
            long mostMoney = 0;
            for (Direction d : Direction.values()) {                         
                MapLocation tryLocation = mapLocation.add(d);                
                if (map.onMap(tryLocation) && gc.canHarvest(id, d)) {   
                    long money = gc.karboniteAt(tryLocation);                                         
                    if (harvestDirection == null || money > mostMoney) {
                        harvestDirection = d;                        
                        mostMoney = money;
                    }                    
                }            
            }
            if (harvestDirection != null) {                
                gc.harvest(id, harvestDirection);            
                done = true;
            }            
        }

        // try to repair neighbors
        // TODO:
        // at this stage (nothing to do)
        // repair adjacent structure that has biggest health gap
        // move towards close a structure needing repairs
        if (!done) {
            for (int i = 0; i < nearbyStructures.size(); i++) {
                Unit nearbyUnit = nearbyStructures.get(i);            
                if (gc.canRepair(id, nearbyUnit.id())) {
                    gc.repair(id, nearbyUnit.id());
                    done = true; 
                    break;
                }
            }
        }

        // if has nothing to do move to most empty areas
        if (!busy) {            
            Direction moveDirection = findMoveDirection(unit);
            if (moveDirection != null && Utils.tryMoveRotate(gc, id, moveDirection)) {
                previousLocation = mapLocation;
            }                                    
        }
    }

    private Direction findMoveDirection(Unit unit) {        
        HashSet<MapLocation> tried = new HashSet<MapLocation>();
        Direction harvestDirection = null;
        int mostEmpty = 0;
        for (Direction d : Utils.directionList) {                        
            MapLocation tryLocation = unit.location().mapLocation().add(d);
            if (previousLocation != null && tryLocation.equals(previousLocation)) {
                continue;
            }
            if (Utils.canOccupy(gc, tryLocation, ((EarthController)parent), tried)) {
                int empty = 0;
                for (Direction dd : Utils.directionList) {
                    MapLocation tryTryLocation = tryLocation.add(dd);
                    if (Utils.canOccupy(gc, tryLocation, ((EarthController)parent), tried)) {
                        empty++;
                    }
                }                
                if (harvestDirection == null || (empty > mostEmpty || (empty == mostEmpty && rng.nextBoolean()))) {
                    harvestDirection = d;
                    mostEmpty = empty;
                }
            }            
        }
        return harvestDirection;
    }

    private Direction findBuildDirection(Unit unit) {
        HashSet<MapLocation> tried = new HashSet<MapLocation>();
        HashSet<MapLocation> triedTried = new HashSet<MapLocation>();
        Direction harvestDirection = null;
        int mostEmpty = 0;                        
        for (Direction d : Utils.directionList) {
            MapLocation tryLocation = unit.location().mapLocation().add(d);
            if (Utils.canOccupy(gc, tryLocation, parent, tried)) {
                if (!testBuildLocation(tryLocation, d, triedTried)) {
                    continue;
                }
                int empty = 0;                
                for (Direction dd : Utils.directionList) {
                    MapLocation tryTryLocation = tryLocation.add(dd);
                    if (Utils.canOccupy(gc, tryLocation, ((EarthController)parent), tried)) {
                        empty++;
                    } 
                }                
                if (harvestDirection == null || (empty > mostEmpty || (empty == mostEmpty && rng.nextBoolean()))) {
                    harvestDirection = d;
                    mostEmpty = empty;
                }
            }            
        }        
        return harvestDirection;
    }

    private boolean testBuildLocation(MapLocation location, Direction direction, HashSet<MapLocation> triedTried) {
        boolean status = true;        
        for (int i = 0; i < Utils.directionList.size(); i++) {
            Direction tryDirection = Utils.directionList.get(i);
            MapLocation tryLocation = location.add(tryDirection);                        
            status = status && Utils.canOccupy(gc, tryLocation, parent, UnitType.Factory, triedTried);   
        }        
        return status;
    }
}