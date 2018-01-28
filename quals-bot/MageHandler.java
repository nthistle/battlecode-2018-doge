import bc.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class MageHandler extends UnitHandler {

    private static final double LATERAL_MOVE_CHANCE = 0.4;
    private boolean lastWasLat = false;

    public MageHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }

    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        
    
    @Override
    public void takeTurn(Unit unit) {
        handleMovement(unit);
        // handleAttack(unit);
    }

    public void handleMovement(Unit unit) {

        MapLocation myLoc = unit.location().mapLocation();
        PlanetMap pmap = gc.startingMap(gc.planet());

        MapLocation target = parent.tm.getTarget(myLoc);
        if(target == null) return;

        PathField pf = parent.pm.getCachedPathField(target);
        if(pf == null) {
            if(parent.tm.getTarget(0) != null)
                pf = parent.pm.getCachedPathField(parent.tm.getTarget(0));
        }
        if(pf == null) return;

        List<Direction> closerDirs = new ArrayList<Direction>();
        List<Direction> sameDirs = new ArrayList<Direction>();

        int myDist = pf.getDistanceAtPoint(myLoc);

        for(Direction dir : Utils.directions()) {
            MapLocation newLoc = myLoc.add(dir);
            if(!pmap.onMap(newLoc)) continue;
            if(pf.getDistanceAtPoint(newLoc) < myDist) {
                closerDirs.add(dir);
            } else if(pf.getDistanceAtPoint(newLoc) == myDist) {
                sameDirs.add(dir);
            }
        }

        boolean movingLat = !lastWasLat && (this.rng.nextDouble() < LATERAL_MOVE_CHANCE);
        this.lastWasLat = false;

        if(movingLat) {
            for(Direction dir : sameDirs) {
                if(gc.canMove(this.id, dir)) {
                    gc.moveRobot(this.id, dir);
                    this.lastWasLat = true;
                    return;
                }
            }
        } else {
            for(Direction dir : closerDirs) {
                if(gc.canMove(this.id, dir)) {
                    gc.moveRobot(this.id, dir);
                    return;
                }
            }
        }


    }

    public Direction getRandomDirection(MapLocation mapLocation, MapLocation targetLocation, PathMaster pm) {
        PathField pf = pm.getPathFieldWithCache(targetLocation);
        PathField.PathPoint pp = pf.getPoint(mapLocation);
        return pp.dirs[rng.nextInt(pp.numDirs)];
    }

    public void handleDeath() {}
}