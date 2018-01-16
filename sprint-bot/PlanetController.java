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
    protected Queue<Swarm> swarmCreationRequest = new LinkedList<>();
    protected Queue<Swarm> swarmRequest = new LinkedList<>();

    public PlanetController(GameController gc, PathMaster pm, Random rng) {
        this.gc = gc;
        this.rng = rng;
        this.pm = pm;
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
    public abstract int getRobotCount(UnitType type);
    public abstract void incrementRobotCount(UnitType type);

    public List<Swarm> getSwarm() {
        return this.swarms;
    }

    public void createSwarm(Swarm type, int num, MapLocation lead, MapLocation target) {
        type.setGoalSize(num);
        type.setSwarmLeader(lead);
        type.setPath(pm.generatePathField(target));
        this.swarmCreationRequest.add(type);
    }

    public abstract void requestSwarm(int num, MapLocation target, UnitType a);

    public Queue<Swarm> getSwarmCreationRequest() {
        return this.swarmCreationRequest;
    }

    public Queue<Swarm> getSwarmRequest() {
        return this.swarmRequest;
    }

    public void setSwarm(ArrayList<Swarm> swarm) {
        this.swarms = swarm;
    }
}