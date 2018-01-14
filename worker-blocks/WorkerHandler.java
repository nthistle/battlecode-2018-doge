import bc.*;
import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;

public class WorkerHandler extends UnitHandler {

    private MapLocation previous = null;

    public WorkerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }

    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        
    
    @Override
    public void takeTurn(Unit unit) {

        boolean building = false;

        MapLocation location = unit.location().mapLocation();

        VecUnit nearby = gc.senseNearbyUnits(location, unit.visionRange());

        for (int j = 0; j < nearby.size(); j++) {
            Unit nearbyUnit = nearby.get(j);
            if (gc.canBuild(unit.id(), nearbyUnit.id())) {                
                System.out.println("Built a factory!");
                building = true;
                gc.build(unit.id(), nearbyUnit.id());                
                break;
            }
        }

        if (((EarthController)parent).robotCount.containsKey(UnitType.Factory) && ((EarthController)parent).robotCount.get(UnitType.Factory) > ((EarthController)parent).robotCount.get(UnitType.Worker) * 4) {
            for (Direction d : Utils.directionList) {
                if (gc.canReplicate(unit.id(), d)) {
                    gc.replicate(unit.id(), d);
                }
            }
        }

        if (!building && gc.karbonite() >= 100) {
            Direction buildDirection = findBuildDirection(unit);
            if (buildDirection != null && gc.canBlueprint(unit.id(), UnitType.Factory, buildDirection)) {
                System.out.println("Blueprinting factory!");
                gc.blueprint(unit.id(), UnitType.Factory, buildDirection);
                building = true;
            }
        }

        if (building) {
            return;
        }

        Direction moveDirection = findMoveDirection(unit);
        if (moveDirection != null && gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), moveDirection)) {
            gc.moveRobot(unit.id(), moveDirection);   
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
}