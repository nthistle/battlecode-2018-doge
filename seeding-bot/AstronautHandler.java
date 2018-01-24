import bc.*;
import java.util.Random;

public class AstronautHandler extends UnitHandler {

	protected final MapLocation myRocketTarget;

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
    	if(!this.parent.pm.isCached(this.myRocketTarget)) {
            // System.out.println("oh no, " + this.myRocketTarget + ", (" + this.myRocketTarget.getX() + "," + this.myRocketTarget.getY() = ")");
    		// means that our rocket took off without us, so let's just reassign ourselves
    		((EarthController)this.parent).myHandler.remove(this.id);
            // System.out.println("Astronaut being reassigned to regular duties.");
    		return;
    	}
    	if(!gc.isMoveReady(this.id))
    		return;
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
}