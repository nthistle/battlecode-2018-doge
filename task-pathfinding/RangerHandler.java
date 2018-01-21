import bc.*;
import java.util.Random;

public class RangerHandler extends UnitHandler {

    private Team enemy;
    private PathField pathToTarget;

    public RangerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        this.enemy = Utils.getOtherTeam(gc.team());
        this.pathToTarget = ((EarthController)parent).getPM().getPathField(((EarthController)parent).getTarget());
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {

    	if(unit.location().isInGarrison()) return;
    	
        // for movement, we're going to use "push forces"
        // enemies that are too close push us away, enemies that are closer to outer shooting range
        // will draw us closer, and allies lightly repel us (causes spread / advance towards enemy)
        
        // for now, going to use something simpler that works
        if(gc.isMoveReady(this.id))
            this.doMovement(unit);
            
        if(gc.isAttackReady(this.id)) {
            // simple enough, shoot at closest robot in range
            
            MapLocation myLocation = unit.location().mapLocation();
            
            VecUnit nearby;
            Unit nearestEnemy;
            long nearestDist;
            
            nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 50, this.enemy); // immediate range
            nearestEnemy = null;
            nearestDist = Integer.MAX_VALUE;
            for(int i = 0; i < nearby.size(); i ++) {
                if(nearestEnemy == null || (myLocation.distanceSquaredTo(
                        nearby.get(i).location().mapLocation()) < nearestDist)) {
                    nearestEnemy = nearby.get(i);
                    nearestDist = myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation());
                }
            }
            if(nearestEnemy != null && gc.canAttack(this.id, nearestEnemy.id())) {
                gc.attack(this.id, nearestEnemy.id());
            }
        }
    }

    private void doMovement(Unit unit) {
    	int tx = unit.location().mapLocation().getX();
    	int ty = unit.location().mapLocation().getY();
    	Utils.tryMoveWiggle(this.gc, this.id, this.pathToTarget.getDirectionsAtPoint(tx, ty)[0]);
    }
}