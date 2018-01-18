import bc.*;
import java.util.Random;

public class MarsRangerHandler extends UnitHandler {

    private Team enemy;

    public MarsRangerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        this.enemy = Utils.getOtherTeam(gc.team());
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
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
    
    // note that this method is not written for efficiency right now
    // TODO write for efficiency
    private void doMovement(Unit unit) {
        // first look for enemy that's too close (<10)
        
        MapLocation myLocation = unit.location().mapLocation();
        
        VecUnit nearby;
        Unit nearestEnemy;
        Unit nearestAlly;
        long nearestDist;
        
        nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 10, this.enemy); // immediate range
        nearestEnemy = null;
        nearestDist = Integer.MAX_VALUE;
        for(int i = 0; i < nearby.size(); i ++) {
            if(nearestEnemy == null || (myLocation.distanceSquaredTo(
                    nearby.get(i).location().mapLocation()) < nearestDist)) {
                nearestEnemy = nearby.get(i);
                nearestDist = myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation());
            }
        }
        if(nearestEnemy != null) {
            System.out.println("Moving away from enemy who is "+nearestDist+" away");
            Utils.tryMoveWiggle(this.gc, this.id, nearestEnemy.location().mapLocation().directionTo(myLocation));
            return;
        }
        
        //move to closest friendly worker nearby
        
        nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 50, gc.team());
        nearestAlly = null;
        nearestDist = Long.MAX_VALUE;
        for(int i = 0; i < nearby.size(); i++) {
        	if(nearestAlly == null
        			|| nearby.get(i).unitType() == UnitType.Worker && myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation()) < nearestDist) {
        		nearestAlly = nearby.get(i);
        		nearestDist = myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation());
        	}
        }
        if(nearestAlly != null) {
        	System.out.println("Moving towards friendly who is "+nearestDist+" away");
        	Utils.tryMoveWiggle(this.gc, this.id, myLocation.directionTo( nearestAlly.location().mapLocation()));
            return;
        }
        
        // no close enemies and no close allies, how about far enemies?
        
        nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 70, this.enemy); // immediate range
        nearestEnemy = null;
        nearestDist = Integer.MAX_VALUE;
        for(int i = 0; i < nearby.size(); i ++) {
            if(nearestEnemy == null || (myLocation.distanceSquaredTo(
                    nearby.get(i).location().mapLocation()) < nearestDist)) {
                nearestEnemy = nearby.get(i);
                nearestDist = myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation());
            }
        }
        if(nearestEnemy != null) {
            System.out.println("Moving towards enemy who is "+nearestDist+" away");
            Utils.tryMoveWiggle(this.gc, this.id, myLocation.directionTo(nearestEnemy.location().mapLocation()));
            return;
        }
        
        // wow... if all this failed, just resort to semirandom movement

        for(int i = 0; i < 5; i ++) {
            Direction moveDir = Utils.getRandomDirection(Direction.values(), this.rng);
            if(gc.canMove(this.id, moveDir)) {
                gc.moveRobot(this.id, moveDir);
                break;
            }
        }
    }
}