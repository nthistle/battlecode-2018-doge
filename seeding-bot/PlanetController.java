import bc.*;
import java.util.Random;

/**
 * Controls all actions just as Player would, but on a specific planet
 * Essentially Player defers control to an instance of this, so that
 * we can separate logic by planet, and so we can pass this as a parent
 * to units, for coordinating unit collabarative actions.
 * @author Neil Thistlethwaite
 * @version 1.0
 */
public abstract class PlanetController
{
    protected final GameController gc;
    protected final Random rng;

    public final PathMaster pm;
    public final TargetingMaster tm;

    public Map<Integer, UnitHandler> myHandler;

    public PlanetController(GameController gc, Random rng) {
        this.gc = gc;
        this.rng = rng;
        
        this.pm = new PathMaster(gc.startingMap(gc.planet()));
        this.tm = new TargetingMaster(gc);
    }
    
    /**
     * The main loop should go inside of this control method.
     * It will be called once.
     */
    public abstract void control();
    
    /**
     * Returns the planet that this controller is responsible
     * for controlling
     */
    public abstract Planet getPlanet();

    /**
     * Gets a reference to this PC's PathMaster (avoids redundant BFS pathing
     * by caching previously requested PathFields)
     */
    public PathMaster getPathMaster() {
        return this.pm;
    }
}