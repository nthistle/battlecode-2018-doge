import bc.*;
import java.util.Random;

public class KnightHandler extends UnitHandler {

    private boolean hasMoved = false;
    private final Team enemy;

    public KnightHandler(GameController gc, int id, Random rng) {
        super(gc, id, rng);
        this.enemy = Utils.getOtherTeam(gc.team());
    }
    
    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
        if(unit.location().isInGarrison()) return;
        hasMoved = false;
        // this is the non-swarm logic
        
        if(gc.isAttackReady(this.id)) {
            
            VecUnit nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 1, this.enemy); // immediate range
            Unit lowestEnemy = null;
            long lowestHealth = Long.MAX_VALUE;
            for(int i = 0; i < nearby.size(); i ++) {
                if(lowestEnemy == null || (nearby.get(i).health() < lowestHealth)) {
                    lowestEnemy = nearby.get(i);
                    lowestHealth = lowestEnemy.health();
                }
            }
            if(lowestEnemy != null) {
                gc.attack(this.id, lowestEnemy.id());
            }
        }

    }
    
    public void setHasMoved(boolean newHasMoved) {
        this.hasMoved = newHasMoved;
    }
    
    // TODO put return codes here for more specificity
    // false -> probably can move, but wasn't able to this turn
    // turn -> any other case
    public boolean attemptSwarm(Unit unit, MapLocation mapLoc) {
        if(unit.location().isInGarrison()) return true;
        if(this.hasMoved || !gc.isMoveReady(this.id))
            return true; // move was already successful
        int retcode = Utils.tryMoveWiggle(this.gc, this.id, unit.location().mapLocation().directionTo(mapLoc));
        if(retcode != 0) {
            this.hasMoved = true;
            return true;
        }
        return false;
    }
}