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
        if(this.hasMoved) 
            unit = gc.unit(this.id); // location doesn't update otherwise
        // this is the non-swarm logic
        
        if(!gc.isAttackReady(this.id))
            return; // everything here is dependent on being ready to attack
        
        VecUnit nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 1, this.enemy); // immediate range
                
        if((!this.hasMoved) && (gc.isMoveReady(this.id)) && (nearby.size()==0)) {
            VecUnit midrange = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 8, this.enemy);
            // sense enemies within range once we move one step towards them
            for(int i = 0; i < midrange.size(); i ++) {
                Direction possDir = unit.location().mapLocation().directionTo(midrange.get(i).location().mapLocation());
                if(gc.canMove(this.id, possDir)) {
                    // move and attack
                    gc.moveRobot(this.id, possDir);
                    if(gc.canAttack(this.id, midrange.get(i).id()))
                        gc.attack(this.id, midrange.get(i).id());
                    return;
                }
            }
        
        } else {
            Unit lowestEnemy = null;
            long lowestHealth = Long.MAX_VALUE;
            for(int i = 0; i < nearby.size(); i ++) {
                if(lowestEnemy == null || (nearby.get(i).health() < lowestHealth)) {
                    lowestEnemy = nearby.get(i);
                    lowestHealth = lowestEnemy.health();
                }
            }
            if(lowestEnemy != null && gc.canAttack(this.id, lowestEnemy.id())) {
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