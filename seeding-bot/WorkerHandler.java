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
// smarter harvesting mechanics
// Mars
// smarter replication code

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
        HashMap<String, Long> moneyCount = earthParent.moneyCount;        
        
        // status markers        
        boolean busy = false;
        boolean done = false;

        VecUnit nearbyAllies = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), gc.team());
        VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), earthParent.enemyTeam);        
        ArrayList<Unit> nearbyStructures = new ArrayList<Unit>(); // ally only        
        ArrayList<Unit> adjacentStructures = new ArrayList<Unit>(); // ally only

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
                PathField path = earthParent.pm.getPathField(tryLocation);
                if (!path.isPointSet(mapLocation)) {
                    continue;
                }
                long distance = path.getDistanceAtPoint(mapLocation);
                if (nearestStructure == null || distance < nearestDistance) {
                    nearestStructure = tryLocation;
                    nearestDistance = distance;
                    structurePath = path;
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
            if (Utils.tryMoveRotate(gc, id, structurePath.getDirectionAtPoint(mapLocation))) {
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
        if (!busy && gc.karbonite() >= 100 && (earthParent.getRobotCount(UnitType.Factory) == 0 || (earthParent.getRobotCount(UnitType.Factory) >= 3 && nearbyStructures.size() == 0) || nearbyStructures.size() > 0)) {
            Direction buildDirection = findBuildDirection(unit);
            if (buildDirection != null && gc.canBlueprint(id, UnitType.Factory, buildDirection)) {
                gc.blueprint(id, UnitType.Factory, buildDirection);
                earthParent.incrementRobotCount(UnitType.Factory);
                busy = true;
                done = true;                
            }
        }                

        // move towards locations of monetary interest
        // TODO: balance between the money and distance
        // if (!busy) {
        //     LinkedList<MapLocation> moneyLocations = earthParent.moneyLocations;
        //     ListIterator<MapLocation> iterator = moneyLocations.listIterator();
        //     PathField moneyPath = null;
        //     MapLocation nearestMoney = null;
        //     nearestDistance = Long.MAX_VALUE;
        //     PathField mostMoneyPath = null;
        //     MapLocation mostMoneyLocation = null;            
        //     long mostMoney = 0;
        //     while (iterator.hasNext()) {
        //         MapLocation tryLocation = iterator.next();                                
        //         PathField path = earthParent.pm.getPathField(tryLocation);
        //         if (!path.isPointSet(mapLocation)) {
        //             continue;
        //         }                
        //         if (tryLocation.isWithinRange(unit.visionRange(), mapLocation)) {
        //             long money = gc.karboniteAt(tryLocation);
        //             if (money <= 0) {
        //                 iterator.remove();
        //                 continue;
        //             }
        //             if (mostMoneyLocation == null || money > mostMoney) {
        //                 mostMoney = money;
        //                 mostMoneyLocation = tryLocation;                        
        //                 moneyPath = path;
        //             }
        //         }                
        //         long distance = path.getDistanceAtPoint(mapLocation);                
        //         if (nearestMoney == null || distance < nearestDistance) {
        //             nearestDistance = distance;
        //             nearestMoney = mostMoneyLocation;
        //             mostMoneyPath = path;
        //         }
        //     }                        
        //     if (mostMoneyLocation != null && Utils.tryMoveRotate(gc, id, mostMoneyPath.getDirectionAtPoint(mapLocation))) {
        //         busy = true;
        //     } else if (nearestMoney != null && Utils.tryMoveRotate(gc, id, moneyPath.getDirectionAtPoint(mapLocation))) {
        //         busy = true;
        //     }            
        // }

        if (!done) {
            Direction harvestDirection = null;
            String harvestLocationKey = null;
            long mostMoney = 0;
            for (Direction d : Direction.values()) {                         
                MapLocation tryLocation = mapLocation.add(d);
                String locationKey = tryLocation.toJson();
                if (moneyCount.containsKey(locationKey)) {
                    long money = gc.karboniteAt(tryLocation);
                    if (money <= 0) {
                        moneyCount.remove(locationKey);
                        continue;
                    } else {
                        moneyCount.put(locationKey, money);       
                        if (gc.canHarvest(id, d) && (harvestDirection == null || money > mostMoney)) {
                            harvestDirection = d;
                            harvestLocationKey = locationKey;
                            mostMoney = money;
                        }
                    }
                }            
            }
            if (harvestDirection != null) {                
                if (mostMoney <= unit.workerHarvestAmount()) {
                    moneyCount.remove(harvestLocationKey);                
                } else {
                    moneyCount.put(harvestLocationKey, mostMoney - unit.workerHarvestAmount());
                }
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