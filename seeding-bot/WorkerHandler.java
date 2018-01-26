import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

// TODO:
// smarter factory placement -> adjacencies and avoid putting on money
// rotation around structure while building
// Mars

public class WorkerHandler extends UnitHandler {

    private Bug bug;    
    private EarthController earthParent;

    public WorkerHandler(PlanetController parent, GameController gc, int id, Random rng) {
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
        Team enemyTeam = earthParent.enemyTeam;
        TargetingMaster tm = earthParent.tm;        
        PathMaster pm = earthParent.pm;
        Map<Integer, UnitHandler> myHandler = earthParent.myHandler;
        Map<UnitType, Integer> robotCount = earthParent.robotCount;        
        
        // status markers        
        boolean busy = false;
        boolean done = false;

        VecUnit nearbyAllies = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), gc.team());
        VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), enemyTeam);        
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
                if (gc.round() < 50) {
                    path = pm.getPathFieldWithCache(tryLocation);
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
                    if (gc.round() < 50) {
                        structurePath = path;   
                    }                    
                }                    
            } else {
                nearbyTroopCount++;
            }
        }

        // if building commit and replicate until 3 workers are present at site
        if (gc.karbonite() >= 60 && nearbyStructures.size() > 0 && nearbyWorkerCount < 3) {
            if (nearestStructure != null && tryReplicateRotate(gc, unit, mapLocation.directionTo(nearestStructure), myHandler)) {
                earthParent.incrementRobotCount(UnitType.Worker);
            } else {
                for (Direction d : Utils.directions()) {
                    if (gc.canReplicate(id, d)) {
                        gc.replicate(id, d);     
                        quickTurn(myHandler, unit);
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
            if (gc.round() < 50 && Utils.tryMoveRotate(gc, id, structurePath.getDirectionAtPoint(mapLocation))) {
                busy = true;
                previousLocation = mapLocation;
            } else if (Utils.tryMoveRotate(gc, id, mapLocation.directionTo(nearestStructure))) {
                busy = true;
                previousLocation = mapLocation;
            }
        }

        // simple rocket build code
        if (!busy && gc.karbonite() >= 150 && earthParent.isSavingForRocket) {
            Direction buildDirection = findBuildDirection(unit);            
            if (buildDirection != null && gc.canBlueprint(id, UnitType.Rocket, buildDirection)) {
                gc.blueprint(id, UnitType.Rocket, buildDirection);
                earthParent.incrementRobotCount(UnitType.Rocket);
                earthParent.isSavingForRocket = false;
                earthParent.rocketsBuilt += 1; // not yet completely accurate
                busy = true;
                done = true;                                    
            }                    
        }        

        // if conditions are appropriate blueprint factory
        long totalStructures = nearbyStructures.size() + nearbyBuiltStructureCount;
        if (!busy && gc.karbonite() >= 200 && (earthParent.getRobotCount(UnitType.Factory) == 0 || (earthParent.getRobotCount(UnitType.Factory) < 3 && nearbyWorkerCount > 1) || (earthParent.getRobotCount(UnitType.Factory) >= 3 && totalStructures == 0) || totalStructures > 0)) {
            Direction buildDirection = findBuildDirection(unit);
            if (buildDirection != null && gc.canBlueprint(id, UnitType.Factory, buildDirection)) {
                gc.blueprint(id, UnitType.Factory, buildDirection);
                earthParent.incrementRobotCount(UnitType.Factory);
                earthParent.isSavingForFactory = false;
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

    private void quickTurn(Map<Integer, UnitHandler> myHandler, Unit unit) {
        earthParent.pm.assignHandler(myHandler, unit);
        myHandler.get(id).takeTurn(unit);        
    }

    private boolean tryReplicateRotate(GameController gc, Unit unit, Direction direction, Map<Integer, UnitHandler> myHandler) {
        int index = Utils.directionList.indexOf(direction);
        for (int i = 0; i < Utils.bigRotation.length; i++) {
            Direction tryDirection = Utils.directionList.get((8 + index + Utils.bigRotation[i]) % 8);
            if (gc.canReplicate(id, tryDirection)) {
                gc.replicate(id, tryDirection);
                quickTurn(myHandler, unit);
                return true;
            }
        }
        return false;
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
            status = status && canOccupyBuild(gc, tryLocation, parent, triedTried);
        }        
        return status;
    }

    public static boolean canOccupyBuild(GameController gc, MapLocation location, PlanetController parent, HashSet<MapLocation> visited) {
        if (visited.contains(location)) {
            return true;
        }
        PlanetMap map = ((EarthController)parent).map;
        boolean status = !map.onMap(location) || map.isPassableTerrainAt(location) == 0 || !gc.hasUnitAtLocation(location) || (gc.senseUnitAtLocation(location).unitType() != UnitType.Factory && gc.senseUnitAtLocation(location).unitType() != UnitType.Rocket);
        if (status) {
            visited.add(location);
        }
        return status;
    }
}