import bc.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class MageHandler extends UnitHandler {

    private static final double LATERAL_MOVE_CHANCE = 0.4;
    private static final int MAGE_CONSIDER_CAP = 10;
    private boolean lastWasLat = false;

    public MageHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }

    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        
    
    @Override
    public void takeTurn(Unit unit) {
        if(gc.isMoveReady(this.id))
            handleMovement(unit);
        if(gc.isAttackReady(this.id))
            handleAttack(unit);
    }

    public void handleAttack(Unit unit) {
        // after movement processing
        MapLocation nLoc = gc.unit(this.id).location().mapLocation();

        VecUnit nearby = gc.senseNearbyUnits(nLoc, unit.attackRange());

        Team team = gc.team();

        int dmg = unit.damage();

        Unit bestTarget = null;
        int bestHeuristic = 0; // pls no negative, rather not hurt friendlies unnecessarily
        int considered = 0;
        for(int i = 0; i < nearby.size() && considered < MAGE_CONSIDER_CAP; i ++) {
            Unit tg = nearby.get(i);
            if(tg.team() == team) return;
            considered ++;
            // assess as potential target
            int nval = assessValue(team, tg, dmg);
            if(nval > bestHeuristic) {
                bestHeuristic = nval;
                bestTarget = tg;
            }
        }

        if(bestTarget == null) return;

        if(gc.canAttack(this.id, bestTarget.id())) {
            gc.attack(this.id, bestTarget.id());
            System.out.println("Mage attacked with a heuristic value of " + bestHeuristic);
        } else {
            System.out.println("MAGE SOMETHING BORKED!");
        }
    }

    public int assessValue(Team us, Unit target, int dmg) {
        VecUnit splash = gc.senseNearbyUnits(target.location().mapLocation(), 2L);
        // everything in splash takes damage
        int enemiesKilled = 0;
        int friendliesKilled = 0;
        int enemyDamage = 0;
        int friendlyDamage = 0;
        for(int i = 0; i < splash.size(); i ++) {
            Unit u = splash.get(i);
            if(u.team() == us) {
                friendlyDamage += dmg;
                if(u.health() <= dmg)
                    friendliesKilled ++;
            } else {
                enemyDamage += dmg;
                if(u.health() <= dmg)
                    enemiesKilled ++;
            }
        }
        return heuristic(enemiesKilled, friendliesKilled, enemyDamage, friendlyDamage);
    }

    // higher heuristic is better
    public int heuristic(int enemiesKilled, int friendliesKilled, int enemyDamage, int friendlyDamage) {
        return (200*enemiesKilled) + (-300*friendliesKilled) + (2*enemyDamage) - (3*friendlyDamage);
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
            for(Direction dir : closerDirs)
                if(Utils.tryMoveWiggle(gc, this.id, dir) > 0)
                    return;
            for(Direction dir : sameDirs)
                if(Utils.tryMoveWiggle(gc, this.id, dir) > 0)
                    return;
        }
    }

    public Direction getRandomDirection(MapLocation mapLocation, MapLocation targetLocation, PathMaster pm) {
        PathField pf = pm.getPathFieldWithCache(targetLocation);
        PathField.PathPoint pp = pf.getPoint(mapLocation);
        return pp.dirs[rng.nextInt(pp.numDirs)];
    }

    public void handleDeath() {}
}