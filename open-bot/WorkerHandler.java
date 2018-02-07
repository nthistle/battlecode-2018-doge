import bc.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

public class WorkerHandler extends UnitHandler {

    private final int TARGET_REFRESH_RATE = 15;

    private Bug bug;    
    private EarthController earthParent;    

    private MapLocation targetLocation = null;
    private int targetTurn = 0;

    private MapLocation previousLocation;

    public boolean solo = false;

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
        MiningMaster mm = earthParent.mm;
        Map<Integer, UnitHandler> myHandler = earthParent.myHandler;        
        
        if (targetLocation != null && (mapLocation.equals(targetLocation) || gc.round() >= targetTurn + TARGET_REFRESH_RATE)) {
            targetLocation = null;
        }

        // status markers        
        boolean busy = false;
        boolean done = false;

        for (Direction d : Direction.values()) {                         
            MapLocation tryLocation = mapLocation.add(d);                
            int tempX = tryLocation.getX();
            int tempY = tryLocation.getY();                
            if (map.onMap(tryLocation)) {
                long money = gc.karboniteAt(tryLocation);                           
                mm.updateIndividual(new Point(tempX, tempY), (int)money);
            }            
        }

        VecUnit nearbyAllies = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), gc.team());
        VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(mapLocation, unit.visionRange(), enemyTeam);        
        ArrayList<Unit> nearbyStructures = new ArrayList<Unit>(); // ally only                

        // count certain robots in vision range for ratio purposes
        // store them as well as precompute nearest
        int nearbyBuiltStructureCount = 0;
        int nearbyWorkerCount = 0;
        int nearbyTroopCount = 0;        
        MapLocation nearestStructure = null;
        long nearest = Long.MAX_VALUE;
        for (int i = 0; i < nearbyAllies.size(); i++) {
            Unit allyUnit = nearbyAllies.get(i);
            UnitType unitType = allyUnit.unitType();
            if (unitType == UnitType.Worker && myHandler.containsKey(allyUnit.id()) && myHandler.get(allyUnit.id()) instanceof WorkerHandler) {
                nearbyWorkerCount++;
            } else if (unitType == UnitType.Factory || unitType == UnitType.Rocket) {                
                MapLocation tryLocation = allyUnit.location().mapLocation();
                if (!pm.isConnected(tryLocation, mapLocation)) {
                    continue;
                }
                if (allyUnit.structureIsBuilt() == 1) {
                    nearbyBuiltStructureCount++;
                    continue;
                }                
                nearbyStructures.add(allyUnit);                
                long distance;                
                if (gc.round() < 50) {
                    PathField path = pm.getPathFieldWithCache(tryLocation);
                    distance = path.getDistanceAtPoint(mapLocation);
                } else {
                    distance = mapLocation.distanceSquaredTo(tryLocation);
                }
                if (nearestStructure == null || distance < nearest) {
                    nearestStructure = tryLocation;
                    nearest = distance;
                }                    
            } else if (unitType != UnitType.Worker) {
                nearbyTroopCount++;
            }
        }

        if (solo && mm.totalValue() < 300) {
            solo = false;
        }
        
        if (gc.karbonite() >= 60 && (solo || (gc.round() < 10 && mm.totalValue() > 500 && earthParent.getEWorkerCount() == earthParent.getRobotCount(UnitType.Worker)))) {
            for (Direction d : Utils.directions()) {
                if (gc.canReplicate(id, d)) {
                    gc.replicate(id, d);     
                    quickTurn(gc, myHandler, mapLocation.add(d), true, mm);
                    earthParent.incrementRobotCount(UnitType.Worker);
                    done = true;
                    solo = false;
                    break;
                }
            }
        }

        if (earthParent.getEWorkerCount() <= 8 && gc.karbonite() >= 60 
            && (((!solo && nearbyBuiltStructureCount == 0 
                && ((earthParent.getEWorkerCount() < 3) 
                || (earthParent.getEWorkerCount() == 3 && nearbyWorkerCount < 3) 
                || (earthParent.getEWorkerCount() == 4 && nearbyWorkerCount == 2)))
            || (nearbyStructures.size() >= 1 && nearbyWorkerCount < 4)))) {                         
            for (Direction d : Utils.directions()) {                
                if (gc.canReplicate(id, d)) {
                    gc.replicate(id, d);     
                    quickTurn(gc, myHandler, mapLocation.add(d), false, mm);
                    earthParent.incrementEWorkerCount();
                    earthParent.incrementRobotCount(UnitType.Worker);
                    done = true;
                    break;
                }
            }            
        }
    
        // potential balancing needed here: stop committing to build if completely outnumbered

        // try to build and commit if possible
        for (int i = 0; i < nearbyStructures.size(); i++) {
            Unit nearbyUnit = nearbyStructures.get(i);            
            if (gc.canBuild(id, nearbyUnit.id())) {
                gc.build(id, nearbyUnit.id());                                
                rotateStructure(mapLocation, nearbyUnit.location().mapLocation(), null);
                busy = true;                
                done = true;
                break;
            }
        }

        // find nearest enemy
        // if necessary TODO: find nearest enemies plural for best escape route
        int nearbyThreatCount = 0;
        MapLocation nearestThreat = null;
        nearest = Long.MAX_VALUE;
        for (int i = 0; i < nearbyEnemies.size(); i++) {
            nearbyThreatCount++;
            Unit nearbyUnit = nearbyEnemies.get(i);
            UnitType type = nearbyUnit.unitType();
            if (type == UnitType.Factory || type == UnitType.Rocket || type == UnitType.Worker) {
                continue;
            }
            MapLocation tryLocation = nearbyUnit.location().mapLocation();
            long distance = mapLocation.distanceSquaredTo(tryLocation);
            if (distance - 2 <= nearbyUnit.attackRange() && distance < nearest) {
                nearestThreat = tryLocation;
                nearest = distance;
            }
        }

        // if within attack range of enemy
        // don't try to assist in building and run in opposite direction
        if (!busy && nearestThreat != null && earthParent.getRobotCount(UnitType.Factory) != 0 && Utils.tryMoveRotate(gc, id, nearestThreat.directionTo(mapLocation))) {
            targetLocation = null;
            busy = true;                        
        }

        // if cannot build but there are nearby structures move towards them
        if (!busy && nearestStructure != null) {
            // System.out.println(nearestStructure);
            move(pm, mapLocation, nearestStructure);
            targetLocation = null;
            busy = true;            
        }

        // simple rocket build code
        if (!busy && gc.karbonite() >= 150 && earthParent.isSavingForRocket) {
            Direction buildDirection = findBuildDirection(unit);            
            if (buildDirection != null && gc.canBlueprint(id, UnitType.Rocket, buildDirection)) {
                MapLocation tempLocation = mapLocation.add(buildDirection);
                int tempX = tempLocation.getX();
                int tempY = tempLocation.getY();
                if (mm.clusterMap[tempX][tempY] != null) {
                    mm.clusterMap[tempX][tempY].update(new Point(tempX, tempY), (int)gc.karboniteAt(tempLocation));
                }
                gc.blueprint(id, UnitType.Rocket, buildDirection);
                earthParent.incrementRobotCount(UnitType.Rocket);
                earthParent.isSavingForRocket = false;
                earthParent.rocketsBuilt += 1; // not yet completely accurate
                busy = true;
                done = true;                                    
            }                    
        }        

        // if conditions are appropriate blueprint factory        
        if (!busy && gc.karbonite() >= 200 && nearbyStructures.size() == 0 && (earthParent.getRobotCount(UnitType.Factory) == 0 || (earthParent.getRobotCount(UnitType.Factory) < 3 && nearbyWorkerCount >= 3) || (earthParent.getRobotCount(UnitType.Factory) >= 3 && nearbyBuiltStructureCount == 0))) {
            Direction buildDirection = findBuildDirection(unit);
            if (buildDirection != null && gc.canBlueprint(id, UnitType.Factory, buildDirection)) {
                MapLocation tempLocation = mapLocation.add(buildDirection);
                int tempX = tempLocation.getX();
                int tempY = tempLocation.getY();
                if (mm.clusterMap[tempX][tempY] != null) {
                    mm.clusterMap[tempX][tempY].update(new Point(tempX, tempY), (int)gc.karboniteAt(tempLocation));
                }
                gc.blueprint(id, UnitType.Factory, buildDirection);
                earthParent.incrementRobotCount(UnitType.Factory);
                earthParent.isSavingForFactory = false;
                busy = true;
                done = true;                
            }
        }

        if (!busy && nearbyWorkerCount > 5 && mm.totalValue() >= 300) {
            quickTurn(gc, myHandler, mapLocation, true, mm);
            earthParent.decrementEWorkerCount();
        }

        if (!busy && targetLocation != null) {
            // System.out.println(targetLocation);
            move(pm, mapLocation, targetLocation);
        }

        int[][] tempMoneyLocations = mm.initialKarboniteLocationsOriginal;
        Direction moveDirection = null;
        if (!(done && busy)) {
            Direction harvestDirection = null;            
            long mostMoney = 0;
            boolean bestChoice = false;
            for (Direction d : Direction.values()) {                         
                MapLocation tryLocation = mapLocation.add(d);                
                int tempX = tryLocation.getX();
                int tempY = tryLocation.getY();                
                if (map.onMap(tryLocation) && gc.canHarvest(id, d)) {                       
                    long money = gc.karboniteAt(tryLocation);           
                    if (tempMoneyLocations[tempX][tempY] != (int)money) {
                        mm.updateIndividual(new Point(tempX, tempY), (int)money);
                    }                              
                    if (harvestDirection == null || money > mostMoney) {
                        harvestDirection = d;                        
                        mostMoney = money;
                        if (gc.canMove(id, d)) {
                            moveDirection = d;
                            bestChoice = true;
                            continue;
                        }
                    }
                    if (!bestChoice && gc.canMove(id, d)) {
                        moveDirection = d;
                    }
                }            
            }
            if (harvestDirection != null) {                
                gc.harvest(id, harvestDirection);            
                done = true;
                MapLocation tempLocation = mapLocation.add(harvestDirection);
                int tempX = tempLocation.getX();
                int tempY = tempLocation.getY();
                if (mm.clusterMap[tempX][tempY] != null) {
                    mm.clusterMap[tempX][tempY].update(new Point(tempX, tempY), (int)unit.workerHarvestAmount());
                }                
            }            
        }
        
        if (!busy && gc.isMoveReady(id)) {
            if (moveDirection != null) {
                gc.moveRobot(id, moveDirection);                
            } else if (targetLocation == null) {
                MapLocation nearestMoney = null;
                int nearestDistance = Integer.MAX_VALUE;   
                int startX = mapLocation.getX() - 7;
                int startY = mapLocation.getY() - 7;
                int endX = startX + 14;
                int endY = startY + 14;
                for (int x = startX; x <= endX; x++) {
                    for (int y = startY; y <= endY; y++) {
                        if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight() || tempMoneyLocations[x][y] <= 0) {
                            continue;
                        }
                        MapLocation tempLocation = new MapLocation(Planet.Earth, x, y);                        
                        if (!pm.isConnected(tempLocation, mapLocation)) {
                            continue;
                        }
                        int distance = (int)mapLocation.distanceSquaredTo(tempLocation);
                        if (tempLocation.isWithinRange(unit.visionRange(), mapLocation) && distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestMoney = tempLocation;
                        }
                    }
                }
                if (nearestMoney != null) {
                    targetLocation = nearestMoney;
                    targetTurn = (int)gc.round();
                    move(pm, mapLocation, nearestMoney);
                }
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
            moveDirection = findMoveDirection(mapLocation);
            if (moveDirection != null && Utils.tryMoveRotate(gc, id, moveDirection)) {
                previousLocation = mapLocation;
            } else if (previousLocation != null && Utils.tryMoveRotate(gc, id, mapLocation.directionTo(previousLocation))) {
                previousLocation = mapLocation;
            }
        }
    }

    public boolean rotateStructure(MapLocation mapLocation, MapLocation structureLocation, HashSet<String> requestLocations) {
        if (!gc.isMoveReady(id)) {
            return false;
        }        
        Direction direction = mapLocation.directionTo(structureLocation);        
        // System.out.println(requestLocations);
        if (bc.bcDirectionIsDiagonal(direction)) {
            Direction left = bc.bcDirectionRotateLeft(direction);
            Direction right = bc.bcDirectionRotateRight(direction);            
            if ((requestLocations == null || !requestLocations.contains(mapLocation.add(left).toJson())) && canOccupyRotate(mapLocation, mapLocation.add(left), structureLocation, requestLocations) && gc.canMove(id, left)) {
                gc.moveRobot(id, left);
                return true;
            }
            if ((requestLocations == null || !requestLocations.contains(mapLocation.add(right).toJson())) && canOccupyRotate(mapLocation, mapLocation.add(right), structureLocation, requestLocations) && gc.canMove(id, right)) {
                gc.moveRobot(id, right);
                return true;
            }
        } else {
            Direction left = bc.bcDirectionRotateLeft(direction);
            Direction right = bc.bcDirectionRotateRight(direction);            
            Direction leftleft = bc.bcDirectionRotateLeft(left);
            Direction rightright = bc.bcDirectionRotateRight(right);
            if ((requestLocations == null || !requestLocations.contains(mapLocation.add(leftleft).toJson())) && canOccupyRotate(mapLocation, mapLocation.add(leftleft), structureLocation, requestLocations) && gc.canMove(id, leftleft)) {
                gc.moveRobot(id, leftleft);
                return true;
            }
            if ((requestLocations == null || !requestLocations.contains(mapLocation.add(rightright).toJson())) && canOccupyRotate(mapLocation, mapLocation.add(rightright), structureLocation, requestLocations) && gc.canMove(id, rightright)) {
                gc.moveRobot(id, rightright);
                return true;
            }
            if ((requestLocations == null || !requestLocations.contains(mapLocation.add(left).toJson())) && canOccupyRotate(mapLocation, mapLocation.add(left), structureLocation, requestLocations) && gc.canMove(id, left)) {
                gc.moveRobot(id, left);
                return true;
            }
            if ((requestLocations == null || !requestLocations.contains(mapLocation.add(right).toJson())) && canOccupyRotate(mapLocation, mapLocation.add(right), structureLocation, requestLocations) && gc.canMove(id, right)) {
                gc.moveRobot(id, right);
                return true;
            }

        }        
        // System.out.println("TEST2");
        return false;
    }

    private void move(PathMaster pm, MapLocation mapLocation, MapLocation destLocation) {
        if (!gc.isMoveReady(id)) {
            return;
        }
        if (pm.isCached(destLocation)) {
            PathField path = pm.getPathFieldWithCache(destLocation);
            if (path.isPointSet(mapLocation)) {
                Utils.tryMoveRotate(gc, id, path.getDirectionAtPoint(mapLocation));
            }
        } else {
            bug.bugMove(mapLocation, destLocation);
        }
    }

    private void quickTurn(GameController gc, Map<Integer, UnitHandler> myHandler, MapLocation newLocation, boolean mining, MiningMaster mm) {
        Unit newWorker = gc.senseUnitAtLocation(newLocation);
        int newId = newWorker.id();        
        if (mining) {
            //TODO chnage to convertToMiner
            //myHandler.put(newId, new MiningWorkerHandler(earthParent, gc, newId, rng, mm));
            if(newWorker.unitType() == UnitType.Worker) {
                // System.out.println("Just requested a brand-new miner");
                mm.convertToMiner(newId);
            }
        } else {
            myHandler.put(newId, new WorkerHandler(earthParent, gc, newId, rng));
        }        
        myHandler.get(newId).takeTurn(newWorker);
    }

    private Direction findMoveDirection(MapLocation mapLocation) {
        HashSet<MapLocation> tried = new HashSet<MapLocation>();
        Direction tryDirection = null;
        int mostEmpty = 0;
        for (Direction d : Utils.directionList) {                        
            MapLocation tryLocation = mapLocation.add(d);
            if (previousLocation != null && tryLocation.equals(previousLocation)) {
                continue;
            }
            if (canOccupyMove(tryLocation, tried)) {
                int empty = 0;
                for (Direction dd : Utils.directionList) {
                    MapLocation tryTryLocation = tryLocation.add(dd);
                    if (canOccupyMove(tryLocation, tried)) {
                        empty++;
                    }
                }                
                if (tryDirection == null || (empty > mostEmpty || (empty == mostEmpty && rng.nextBoolean()))) {
                    tryDirection = d;
                    mostEmpty = empty;
                }
            }            
        }
        return tryDirection;
    }

    private Direction findBuildDirection(Unit unit) {
        HashSet<MapLocation> tried = new HashSet<MapLocation>();
        HashSet<MapLocation> triedTried = new HashSet<MapLocation>();
        Direction tryDirection = null;
        int mostEmpty = 0;                        
        for (Direction d : Utils.directionList) {
            MapLocation tryLocation = unit.location().mapLocation().add(d);
            if (canOccupyMove(tryLocation, tried)) {
                if (!testBuildLocation(tryLocation, d, triedTried)) {
                    continue;
                }
                int empty = 0;                
                for (Direction dd : Utils.directionList) {
                    MapLocation tryTryLocation = tryLocation.add(dd);
                    if (canOccupyMove(tryTryLocation, tried)) {
                        empty++;
                    } 
                }                
                if (tryDirection == null || (empty > mostEmpty || (empty == mostEmpty && rng.nextBoolean()))) {
                    tryDirection = d;
                    mostEmpty = empty;
                }
            }            
        }        
        return tryDirection;
    }

    private boolean testBuildLocation(MapLocation location, Direction direction, HashSet<MapLocation> triedTried) {
        if (earthParent.getRobotCount(UnitType.Factory) == 0 && gc.round() > 100) {
            return true;
        }
        if (bc.bcDirectionIsDiagonal(direction)) {
            Direction left = bc.bcDirectionRotateLeft(direction);
            Direction right = bc.bcDirectionRotateRight(direction);
            MapLocation upper = location.add(direction);
            MapLocation leftUpper = location.add(left);
            MapLocation rightUpper = location.add(right);
            left = bc.bcDirectionRotateLeft(left);
            right = bc.bcDirectionRotateRight(right);
            MapLocation leftSide = location.add(left);
            MapLocation rightSide = location.add(right);
            left = bc.bcDirectionRotateLeft(left);
            right = bc.bcDirectionRotateRight(right);   
            MapLocation leftLower = location.add(left);
            MapLocation rightLower = location.add(right);         
            if (canOccupy(upper) && !(canOccupy(leftLower) && canOccupy(leftUpper)) && !(canOccupy(rightLower) && canOccupy(rightUpper))) {
                return false;
            }
            if (canOccupy(leftUpper) && !canOccupy(leftLower) && !(canOccupy(rightLower) && canOccupy(rightUpper))) {
                return false;
            }
            if (canOccupy(rightUpper) && !canOccupy(rightLower) && !(canOccupy(leftLower) && canOccupy(leftUpper))) {
                return false;
            }            
            if (canOccupy(leftSide) && !canOccupy(leftLower) && !(canOccupy(rightLower) && canOccupy(rightUpper) && canOccupy(leftUpper))) {
                return false;
            }
            if (canOccupy(rightSide) && !canOccupy(rightLower) && !(canOccupy(leftLower) && canOccupy(leftUpper) && canOccupy(rightUpper))) {
                return false;
            }
        } else {
            Direction left = bc.bcDirectionRotateLeft(direction);
            Direction right = bc.bcDirectionRotateRight(direction);
            MapLocation upper = location.add(direction);
            MapLocation leftUpper = location.add(left);
            MapLocation rightUpper = location.add(right);
            left = bc.bcDirectionRotateLeft(left);
            right = bc.bcDirectionRotateRight(right);
            MapLocation leftSide = location.add(left);
            MapLocation rightSide = location.add(right);
            if (canOccupy(upper) && !canOccupy(leftSide) && !canOccupy(rightSide)) {
                return false;
            }
            if (canOccupy(leftUpper) && !canOccupy(leftSide) && !(canOccupy(rightSide) && canOccupy(upper))) {
                return false;
            }
            if (canOccupy(rightUpper) && !canOccupy(rightSide) && !(canOccupy(leftSide) && canOccupy(upper))) {
                return false;
            }            
        }
        boolean status = true;        
        for (int i = 0; i < Utils.directionList.size(); i++) {
            Direction tryDirection = Utils.directionList.get(i);
            MapLocation tryLocation = location.add(tryDirection);                        
            status = status && canOccupyBuild(tryLocation, triedTried);
        }        
        return status;
    }

    private boolean canOccupy(MapLocation location) {
        PlanetMap map = earthParent.map;
        return map.onMap(location) && map.isPassableTerrainAt(location) == 1 && !(gc.hasUnitAtLocation(location) && gc.senseUnitAtLocation(location).unitType() == UnitType.Factory);        
    }


    private boolean canOccupyMove(MapLocation location, HashSet<MapLocation> visited) {
        if (visited.contains(location)) {            
            return true;
        }
        PlanetMap map = earthParent.map;
        boolean status = map.onMap(location) && map.isPassableTerrainAt(location) == 1 && !gc.hasUnitAtLocation(location);
        if (status) {
            visited.add(location);
        }
        return status;
    }

    private boolean canOccupyRotate(MapLocation mapLocation, MapLocation location, MapLocation structureLocation, HashSet<String> requestLocations) {
        PlanetMap map = earthParent.map;
        if (!map.onMap(location) || map.isPassableTerrainAt(location) == 0 || (gc.hasUnitAtLocation(location) && (gc.senseUnitAtLocation(location).team() != gc.team() || gc.senseUnitAtLocation(location).unitType() != UnitType.Worker))) {
            return false;
        }        
        for (Direction d : Utils.directionList) {
            MapLocation tryLocation = location.add(d);
            if (!map.onMap(tryLocation) || map.isPassableTerrainAt(tryLocation) == 0 || tryLocation.equals(structureLocation)) {
                continue;
            }
            if (!tryLocation.isAdjacentTo(structureLocation) && gc.hasUnitAtLocation(tryLocation) && gc.senseUnitAtLocation(tryLocation).unitType() == UnitType.Worker) {
                return false;
            }
        }
        if (!gc.hasUnitAtLocation(location)) {
            return true;
        } else {
            UnitHandler tryWorker = earthParent.myHandler.get(gc.senseUnitAtLocation(location).id());
            if (tryWorker instanceof MiningWorkerHandler) {
                return false;
            }
            WorkerHandler worker = (WorkerHandler)tryWorker;
            Unit workerUnit = gc.unit(worker.id);
            if (requestLocations == null) {
                requestLocations = new HashSet<String>();                
            }
            requestLocations.add(mapLocation.toJson());
            if (worker.rotateStructure(workerUnit.location().mapLocation(), structureLocation, requestLocations)) {
                return true;
            }
        }
        return false;
    }

    private boolean canOccupyBuild(MapLocation location, HashSet<MapLocation> visited) {
        if (visited.contains(location)) {
            return true;
        }
        PlanetMap map = earthParent.map;
        boolean status = !map.onMap(location) || map.isPassableTerrainAt(location) == 0 || !gc.hasUnitAtLocation(location) || (gc.senseUnitAtLocation(location).unitType() != UnitType.Factory && gc.senseUnitAtLocation(location).unitType() != UnitType.Rocket);
        if (status) {
            visited.add(location);
        }
        return status;
    }

    public void handleDeath() {}
}