import bc.*;
import java.util.Random;

public class RangerHandler extends UnitHandler {

    public RangerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
        if (!unit.location().isOnMap()) {
            return;
        }

        if(gc.isAttackReady(this.id)) {
            // simple enough, shoot at closest robot in range
            
            MapLocation myLocation = unit.location().mapLocation();
            
            VecUnit nearby;
            Unit nearestEnemy;
            long nearestDist;
            
            nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 50, Utils.getOtherTeam(gc.team())); // immediate range
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
}