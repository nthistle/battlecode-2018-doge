import bc.*;
import java.util.Random;

public class AstronautHandler extends UnitHandler {

	protected final MapLocation myRocketTarget;
    protected int timer = -1;

    public AstronautHandler(PlanetController parent, GameController gc, int id, Random rng, MapLocation target) {
        super(parent, gc, id, rng);
        this.myRocketTarget = target;
        // System.out.println("Astronaut Instantiated!");
    }
    
    public void takeTurn() {
    	this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
        if(timer > 0) timer --;
    	if(timer == 0 || !this.parent.pm.isCached(this.myRocketTarget)) { // if we aren't wanted, or out rocket took off already
            // System.out.println("oh no, " + this.myRocketTarget + ", (" + this.myRocketTarget.getX() + "," + this.myRocketTarget.getY() = ")");
    		// means that our rocket took off without us, so let's just reassign ourselves
    		((EarthController)this.parent).myHandler.remove(this.id);
            // System.out.println("Astronaut being reassigned to regular duties.");
    		return;
    	}
    	if(!gc.isMoveReady(this.id))
    		return;
        int myDist = this.parent.pm.getCachedPathField(this.myRocketTarget).getDistanceAtPoint(unit.location().mapLocation());
        if(myDist == 1 && timer < 0) timer = 3; // we give the rocket 3 turns to load us, then assume we are unwanted
        // TODO probably leave this in, but in the future instead have this check and ask the rocket if it still wants us
        // (make it pass the rockethandler)
    	Direction[] validDirs = this.parent.pm.getCachedPathField(this.myRocketTarget).getDirectionsAtPoint(unit.location().mapLocation());
    	for(Direction d : validDirs) {
            if(d==null)
                return;
	    	if(gc.canMove(this.id, d)) {
	    		gc.moveRobot(this.id, d);
                return;
	    	}
	    }
    }

    public void handleDeath() {}
}