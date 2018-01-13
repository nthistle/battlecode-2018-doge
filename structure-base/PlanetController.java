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

    public PlanetController(GameController gc, Random rng) {
        this.gc = gc;
        this.rng = rng;
    }
    
    /**
     * The main loop should go inside of this control method.
     * It will be called once.
     */
    public abstract void control();
}