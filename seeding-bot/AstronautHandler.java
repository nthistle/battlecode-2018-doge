import bc.*;
import java.util.Random;

public class AstronautHandler extends UnitHandler {

	protected final MapLocation myRocketTarget;

    public AstronautHandler(PlanetController parent, GameController gc, int id, Random rng, MapLocation target) {
        super(parent, gc, id, rng);
        this.myRocketTarget = target;
    }
    
    public void takeTurn() {
    	this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
    	if(!this.parent.pm.isCached(this.myRocketTarget)) {
    		// means that our rocket took off without us, so let's just reassign ourselves
    		this.parent.myHandler.remove(this.id);
    		return;
    	}
    	if(!gc.isMoveReady(this.id))
    		return;
    	Direction[] validDirs = this.parent.pm.getCachedPathField(this.myRocketTarget).getDirectionsAtPoint(unit.location().mapLocation());
    	for(Direction d : validDirs) {
	    	if(gc.canMove(this.id, d)) {
	    		gc.moveRobot(this.id, d);
	    	}
	    }
    }
}