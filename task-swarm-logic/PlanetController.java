import bc.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public abstract class PlanetController
{
    protected final GameController gc;
    protected final Random rng;
    protected List<Swarm> swarms;

    public PlanetController(GameController gc, Random rng) {
        this.gc = gc;
        this.rng = rng;
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

    public List<Swarm> getSwarm() {
        return this.swarms;
    }

    public void setSwarm(ArrayList<Swarm> swarm) {
        this.swarms = swarm;
    }
}