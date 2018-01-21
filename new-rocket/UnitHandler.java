import bc.*;
import java.util.Random;

public abstract class UnitHandler {
    
    protected final PlanetController parent;
    protected final GameController gc;
    protected final Random rng;
    protected final int id;
    
    protected MapLocation target;
    protected boolean rocketBound;

    public UnitHandler(PlanetController parent, GameController gc, int id, Random rng) {
        this.parent = parent;
        this.gc = gc;
        this.id = id;
        this.rng = rng;
        this.target = null;
        this.rocketBound = false;
    }
    
    public void setTarget(MapLocation target) {
    	this.target = target;
    }
    
    public MapLocation getTarget() {
    	return this.target;
    }
    
    public void makeRocketBound(boolean setting) {
    	this.rocketBound = setting;
    }
    
    public boolean isRocketBound() {
    	return this.rocketBound;
    }
    
    // subclasses required to override; if they prefer to use the
    // method that takes unit as a parameter, this may be replaced
    // with simply a call to that with gc.unit(id) as the argument
    public abstract void takeTurn();
    
    // subclasses can optionally override this if they want 
    // more convenient access to the unit (rather than gc.unit(id))
    // this will be called by player in the loop
    public void takeTurn(Unit unit) {
        this.takeTurn();
    }
}