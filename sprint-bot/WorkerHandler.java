import bc.*;
import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.HashMap;

public class WorkerHandler extends UnitHandler {

    private MapLocation targetLocation = null;    
    private MapLocation previous = null;    

    public WorkerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }
    
    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        
    
    @Override
    public void takeTurn(Unit unit) {        
        try{
        if (!unit.location().isOnMap()) {            
            return;
        }

        if (unit.location().isOnPlanet(Planet.Mars)) {
            if (gc.round() >= 750) {
                for (Direction d : Utils.directionList) {
                    if (gc.canReplicate(unit.id(), d)) {
                        gc.replicate(unit.id(), d);                             
                        break;
                    }
                }                
            }            
            return;
        }

        boolean stationary = false;        

        PlanetMap map = ((EarthController)parent).earthMap;
        HashMap<String, Long> moneyCount = ((EarthController)parent).moneyCount;        

        MapLocation location = unit.location().mapLocation();        

        VecUnit nearbyFriendly = gc.senseNearbyUnitsByTeam(location, unit.visionRange(), gc.team());
        VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(location, unit.visionRange(), Utils.getOtherTeam(gc.team()));

        int nearbyFactoryCount = 0;
        int nearbyBuiltFactoryCount = 0;
        int nearbyWorkerCount = 0;
        for (int i = 0; i < nearbyFriendly.size(); i++) {
            if (nearbyFriendly.get(i).unitType() == UnitType.Worker) {
                nearbyWorkerCount++;
            } else if (nearbyFriendly.get(i).unitType() == UnitType.Factory) {
                nearbyFactoryCount++;
                if (nearbyFriendly.get(i).structureIsBuilt() == 1) {
                    nearbyBuiltFactoryCount++;
                }
            }
        }

        if (nearbyFactoryCount > 0 && nearbyWorkerCount < 3) { 
            // System.out.println("Early game replication");
            for (Direction d : Utils.directionList) {
                if (gc.canReplicate(unit.id(), d)) {
                    gc.replicate(unit.id(), d);     
                    parent.incrementRobotCount(UnitType.Worker);
                    break;
                }
            }
        }

        if (gc.round() > 600 || gc.getTimeLeftMs() < 1000 ) {
            try{
            VecUnit nearbyRockets = gc.senseNearbyUnitsByType(location, unit.visionRange(), UnitType.Rocket);
            Unit nearestRocket = null;
            int nearestDistanceRocket = Integer.MAX_VALUE;
            for (int j = 0; j < nearbyRockets.size(); j++) {
                Unit nearbyUnit = nearbyRockets.get(j);
                PathField path = parent.pm.getPathField(nearbyUnit.location().mapLocation());
                if (nearbyUnit.structureIsBuilt() == 1 || !path.isPointSet(location)) {
                    continue;
                }
                if (gc.canBuild(unit.id(), nearbyUnit.id())) {                
                    // System.out.println("Building factory!");                
                    gc.build(unit.id(), nearbyUnit.id());   
                    stationary = true;             
                    break;
                }
                if (nearbyUnit.team() == gc.team()) {
                    int distance = (int)location.distanceSquaredTo(nearbyUnit.location().mapLocation());
                    if (distance < nearestDistanceRocket || (distance == nearestDistanceRocket && rng.nextBoolean())) {
                        nearestRocket = nearbyUnit;
                        nearestDistanceRocket = distance;
                    }
                }
            }            
            if (!stationary && nearestRocket != null) {
                MapLocation target = nearestRocket.location().mapLocation();
                // PathField path = parent.pm.getPathField(target);
                if (Utils.tryMoveRotate(gc, unit, location.directionTo(target)) != -1) {
                    stationary = true;                    
                }
            }      
            if (stationary) {
                      return;
                  }      
            for (Direction d : Utils.directionList) {  
                if (gc.canBlueprint(unit.id(), UnitType.Rocket, d)) {
                    gc.blueprint(unit.id(), UnitType.Rocket, d);
                    break;
                }                
            }        
            return;
            }catch(Exception e){}    
        }        

        VecUnit nearbyFactories = gc.senseNearbyUnitsByType(location, unit.visionRange(), UnitType.Factory);
        Unit nearestFactory = null;
        int nearestDistanceFactory = Integer.MAX_VALUE;
        for (int j = 0; j < nearbyFactories.size(); j++) {
            Unit nearbyUnit = nearbyFactories.get(j);
            PathField path = parent.pm.getPathField(nearbyUnit.location().mapLocation());
            if (nearbyUnit.structureIsBuilt() == 1 || !path.isPointSet(location)) {
                continue;
            }
            if (gc.canBuild(unit.id(), nearbyUnit.id())) {                
                // System.out.println("Building factory!");                
                gc.build(unit.id(), nearbyUnit.id());   
                stationary = true;             
                break;
            }
            if (nearbyUnit.team() == gc.team()) {
                int distance = (int)location.distanceSquaredTo(nearbyUnit.location().mapLocation());
                if (distance < nearestDistanceFactory || (distance == nearestDistanceFactory && rng.nextBoolean())) {
                    nearestFactory = nearbyUnit;
                    nearestDistanceFactory = distance;
                }
            }
        }
        if (!stationary && nearestFactory != null) {
            MapLocation target = nearestFactory.location().mapLocation();
            // PathField path = parent.pm.getPathField(target);
            if (Utils.tryMoveRotate(gc, unit, location.directionTo(target)) != -1) {
                stationary = true;
                previous = location;                
            }
        }

        if (!stationary && nearbyEnemies.size() >= (int)((nearbyFriendly.size() - nearbyFactoryCount - nearbyWorkerCount) * 1.5)) {
            Unit nearestEnemy = null;
            int nearestDistanceEnemy = Integer.MAX_VALUE;
            for (int j = 0; j < nearbyEnemies.size(); j++) {
                Unit nearbyUnit = nearbyEnemies.get(j);                                
                int distance = (int)location.distanceSquaredTo(nearbyUnit.location().mapLocation());
                if (distance < nearestDistanceEnemy || (distance == nearestDistanceEnemy && rng.nextBoolean())) {
                    nearestEnemy = nearbyUnit;
                    nearestDistanceEnemy = distance;
                }                
            }            
            if (!stationary && nearestEnemy != null && Utils.tryMoveRotate(gc, unit, location.directionTo(location.subtract(location.directionTo(nearestEnemy.location().mapLocation())))) != -1) {
                stationary = true;
            }            
        }

        if (!stationary && gc.karbonite() >= 100 && (parent.getRobotCount(UnitType.Factory) == 0 || (parent.getRobotCount(UnitType.Factory) >= 3 && nearbyFactoryCount == 0) || nearbyFactoryCount > 0)) {
            Direction buildDirection = findBuildDirection(unit);            
            if (buildDirection != null && gc.canBlueprint(unit.id(), UnitType.Factory, buildDirection)) {
                // System.out.println("Blueprinting factory!");
                gc.blueprint(unit.id(), UnitType.Factory, buildDirection);
                parent.incrementRobotCount(UnitType.Factory);
                stationary = true;                
            }
        }                

        if (!stationary) {
            LinkedList<MapLocation> moneyLocations = ((EarthController)parent).moneyLocations;
            ListIterator<MapLocation> iterator = moneyLocations.listIterator();
            MapLocation nearestMoney = null;
            long nearestDistance = Integer.MAX_VALUE;            
            MapLocation mostMoneyLocation = null;
            long mostMoney = 0;
            while (iterator.hasNext()) {
                MapLocation tryLocation = iterator.next();
                if (tryLocation.isWithinRange(unit.visionRange(), location)) {
                    long money = gc.karboniteAt(tryLocation);
                    if (money <= 0) {
                        iterator.remove();
                        continue;
                    }
                    if (mostMoneyLocation == null || money > mostMoney) {
                        mostMoney = money;
                        mostMoneyLocation = tryLocation;                        
                    }
                }
                long distance = location.distanceSquaredTo(tryLocation);
                if (nearestMoney == null || distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestMoney = mostMoneyLocation;
                }
            }            
            if (mostMoneyLocation != null && Utils.tryMoveRotate(gc, unit, location.directionTo(mostMoneyLocation)) != -1) {
                stationary = true;
            } else if (nearestMoney != null && Utils.tryMoveRotate(gc, unit, location.directionTo(nearestMoney)) != -1) {
                stationary = true;
            }
            // try {
                // MapLocation nearestMoney = null;
                // int nearestDistance = Integer.MAX_VALUE;            
                // for (String locationKey : moneyCount.keySet()) {                
                //     MapLocation tryLocation = bc.bcMapLocationFromJson(locationKey);                
                //     // long distance = location.distanceSquaredTo(tryLocation);
                //     // if (distance < nearestDistance || (distance == nearestDistance && rng.nextBoolean())) {
                //     //     nearestMoney = tryLocation;
                //     //     nearestDistance = distance;
                //     // }
                //     PathField path = parent.pm.getPathField(tryLocation);
                //     if (path.isPointSet(location)) {                    
                //         int distance = path.getDistanceAtPoint(location);                    
                //         if (distance < nearestDistance) {                        
                //             nearestMoney = tryLocation;
                //             nearestDistance = distance;                        
                //         }
                //     }                
                // }                

                // if (nearestMoney != null) {
                //     Direction d = parent.pm.getPathField(nearestMoney).getDirectionAtPoint(location);
                //     // System.out.println("test");
                //     if (Utils.tryMoveRotate(gc, unit, d) != -1) {
                //         stationary = true;                    
                //     }
                // }
            // } catch (Exception e) {e.printStackTrace();}
        }

        Direction bestDirection = null;
        String bestLocationKey = null;
        long mostMoney = 0;
        for (Direction d : Direction.values()) {                         
            MapLocation tryLocation = location.add(d);
            String locationKey = tryLocation.toJson();            
            if (moneyCount.containsKey(locationKey)) {
                long money = gc.karboniteAt(tryLocation);
                if (money <= 0) {
                    moneyCount.remove(locationKey);
                    continue;
                } else {
                    moneyCount.put(locationKey, money);       
                    if (gc.canHarvest(unit.id(), d) && (bestDirection == null || money > mostMoney)) {
                        bestDirection = d;
                        bestLocationKey = locationKey;
                        mostMoney = money;
                    }
                }
            }            
        }
        if (bestDirection != null) {
            // System.out.println("Harvesting");
            gc.harvest(unit.id(), bestDirection);            
            if (mostMoney <= unit.workerHarvestAmount()) {
                moneyCount.remove(bestLocationKey);                
            } else {
                moneyCount.put(bestLocationKey, mostMoney - unit.workerHarvestAmount());
            }            
        }

        // VecUnit nbors = gc.senseNearbyUnitsByType(unit.location().mapLocation(), 2, UnitType.Factory);
        // for(int i = 0; i < nbors.size(); i ++) {
        //     if(nbors.get(i).team() != gc.team()) continue;
        //     if(gc.canRepair(this.id, nbors.get(i))) {
        //         gc.repair(this.id, nbors.get(i));
        //         break;
        //     }
        // }

        if (stationary) {
            return;
        }

        Direction moveDirection = findMoveDirection(unit);
        if (moveDirection != null && Utils.tryMoveRotate(gc, unit, moveDirection) != -1) {
            previous = location;
        }                
        }catch(Exception e){};
    }

    private Direction findMoveDirection(Unit unit) {        
        HashSet<MapLocation> tried = new HashSet<MapLocation>();
        Direction bestDirection = null;
        int mostEmpty = 0;
        for (Direction d : Utils.directionList) {                        
            MapLocation tryLocation = unit.location().mapLocation().add(d);                
            if (previous != null && tryLocation.equals(previous)) {
                continue;
            }
            if (Utils.canOccupy(gc, tryLocation, parent, tried)) {
                int empty = 0;
                for (Direction dd : Utils.directionList) {
                    MapLocation tryTryLocation = tryLocation.add(dd);
                    if (Utils.canOccupy(gc, tryLocation, parent, tried)) {
                        empty++;
                    }
                }                
                if (bestDirection == null || (empty > mostEmpty || (empty == mostEmpty && rng.nextBoolean()))) {
                    bestDirection = d;
                    mostEmpty = empty;
                }
            }            
        }
        return bestDirection;
    }

    private Direction findBuildDirection(Unit unit) {
        HashSet<MapLocation> tried = new HashSet<MapLocation>();
        HashSet<MapLocation> triedTried = new HashSet<MapLocation>();
        Direction bestDirection = null;
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
                    if (Utils.canOccupy(gc, tryLocation, parent, tried)) {
                        empty++;
                    } 
                }                
                if (bestDirection == null || (empty > mostEmpty || (empty == mostEmpty && rng.nextBoolean()))) {
                    bestDirection = d;
                    mostEmpty = empty;
                }
            }            
        }        
        return bestDirection;
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