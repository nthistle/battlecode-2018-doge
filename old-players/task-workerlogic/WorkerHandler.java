import bc.*;
import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;

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

        if (!unit.location().isOnMap()) {
            continue;
        }

        boolean stationary = false;        

        MapLocation location = unit.location().mapLocation();

        VecUnit nearbyFriendly = gc.senseNearbyUnitsByTeam(location, unit.visionRange(), gc.team());
        VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(location, unit.visionRange(), Utils.getOtherTeam(gc.team()));

        if (nearbyEnemies.size() >= (int)(nearbyFriendly.size() * 1.5)) {
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

        if (parent.getRobotCount(UnitType.Factory) == 1 && (parent.getRobotCount(UnitType.Worker) == 1 || (parent.getRobotCount(UnitType.Factory) <= parent.getRobotCount(UnitType.Worker) && gc.senseNearbyUnitsByType(location, unit.visionRange(), UnitType.Worker).size() <= 1))) {
            // System.out.println("Early game replication");
            for (Direction d : Utils.directionList) {
                if (gc.canReplicate(unit.id(), d)) {
                    gc.replicate(unit.id(), d);
                    System.out.println(gc.units().get(gc.units().size() - 1));
                    parent.incrementRobotCount(UnitType.Worker);
                    break;
                }
            }
        }

        VecUnit nearbyFactories = gc.senseNearbyUnitsByType(location, unit.visionRange(), UnitType.Factory);
        Unit nearestFactory = null;
        int nearestDistanceFactory = Integer.MAX_VALUE;
        for (int j = 0; j < nearbyFactories.size(); j++) {
            Unit nearbyUnit = nearbyFactories.get(j);
            if (nearbyUnit.structureIsBuilt() == 1) {
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

        if (!stationary && nearestFactory != null && Utils.tryMoveRotate(gc, unit, location.directionTo(nearestFactory.location().mapLocation())) != -1) {
            stationary = true;
        }

        if (!stationary && gc.karbonite() >= 100) {
            Direction buildDirection = findBuildDirection(unit);
            if (buildDirection != null && gc.canBlueprint(unit.id(), UnitType.Factory, buildDirection)) {
                // System.out.println("Blueprinting factory!");
                gc.blueprint(unit.id(), UnitType.Factory, buildDirection);
                parent.incrementRobotCount(UnitType.Factory);
                stationary = true;
            }
        }

        if (stationary) {
            return;
        }

        Direction moveDirection = findMoveDirection(unit);
        if (moveDirection != null && Utils.tryMoveRotate(gc, unit, moveDirection) != -1) {
            previous = location;
        }                
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

    // HARDCODED_FIRST_COUPLE_OF_ROUNDS

    //                 Direction bad = null;
    //         for (Direction d : Utils.directionList) {

    //             if(gc.canReplicate(unit.id(), d)){                    
    //                 bad = d;
    //                 break;
    //             }}

    //         gc.blueprint(unit.id(), UnitType.Factory, Utils.directionList.get((8 + Utils.directionList.indexOf(bad) + 1) % 8));
    //         Unit factory = gc.senseUnitAtLocation(unit.location().mapLocation().add(Utils.directionList.get((8 + Utils.directionList.indexOf(bad) + 1) % 8)));
    //         gc.nextTurn();
    //         gc.build(unit.id(), factory.id());
    //         System.out.println(gc.karbonite());
    //         gc.nextTurn();
    //         gc.build(unit.id(), factory.id());

    //         System.out.println(gc.karbonite());
    // gc.nextTurn();
    // gc.replicate(unit.id(), bad);
    //                 Unit replicated = gc.senseUnitAtLocation(unit.location().mapLocation().add(bad));
    // System.out.println(gc.canBuild(replicated.id(), factory.id()));                
    // gc.build(replicated.id(), factory.id());
    //             gc.nextTurn();                
    //             gc.build(unit.id(), factory.id());
    //             gc.build(replicated.id(), factory.id());
    //             System.out.println(gc.karbonite());
    //             gc.nextTurn();
    //             gc.build(unit.id(), factory.id());
    //             gc.build(replicated.id(), factory.id());

    // gc.nextTurn();
    //         Direction temp = Utils.directionList.get((8 + Utils.directionList.indexOf(bad)  - 1) % 8);
    //             gc.replicate(replicated.id(), temp);
    //                 Unit Karthik = gc.senseUnitAtLocation(unit.location().mapLocation().add(temp));
    //                 Utils.tryMoveRotate(gc, Karthik, Karthik.location().mapLocation().directionTo(factory.location().mapLocation()));
    // gc.build(Karthik.id(), factory.id());
    //     gc.build(unit.id(), factory.id());
    //             while (true) {
    //                 if (gc.canBuild(unit.id(), factory.id())) {
    //                     gc.build(unit.id(), factory.id());
    //                 } else {
    //                     System.out.println(1+" " + gc.round());
    // return ;
    //                 }
    //                 if (gc.canBuild(replicated.id(), factory.id())) {                    
    //                     gc.build(replicated.id(), factory.id());
    //                 } else {
    //                     System.out.println(2 + " " + gc.round());return ;
    //                 }
    //                 if (gc.canBuild(Karthik.id(), factory.id())) {                    
    //                     gc.build(Karthik.id(), factory.id());
    //                 } else {
    //                     System.out.println(3 + " " + gc.round());return ;
    //                 }

    //                 gc.nextTurn();


    //             }


}