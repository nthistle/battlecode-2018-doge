import bc.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public abstract class PlanetController
{
    protected final GameController gc;
    protected final Random rng;
    protected List<Swarm> swarms;

    public PlanetController(GameController gc, Random rng, List<Swarm> swarms) {
        this.gc = gc;
        this.rng = rng;
        this.swarms = swarms;
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
}