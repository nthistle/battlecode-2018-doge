import bc.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public abstract class PlanetController
{
    protected final GameController gc;
    protected final PathMaster pm;
    protected final Random rng;
    protected List<Swarm> swarms;
    protected Queue<Swarm> swarmRequest = new LinkedList<>();

    public PlanetController(GameController gc, Random rng) {
        this.gc = gc;
        this.rng = rng;
        pm = new PathMaster(this.gc.startingMap(this.gc.planet()));
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

    public void createSwarm(Swarm type, int num, MapLocation lead, MapLocation target) {
        type.setGoalSize(num);
        type.setSwarmLeader(lead);
        type.setPath(pm.generatePathField(target));
        this.swarmRequest.add(type);
    }

    public Queue<Swarm> getSwarmRequest() {
        return this.swarmRequest;
    }

    public void setSwarm(ArrayList<Swarm> swarm) {
        this.swarms = swarm;
    }
}