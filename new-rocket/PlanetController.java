import bc.*;
import java.util.*;

public abstract class PlanetController
{
    protected final GameController gc;
    protected final PathMaster pm;
    protected final Random rng;
    
    protected List<Integer> factories;
    protected List<Integer> rockets;
    protected List<Integer> rangers;
    protected List<Integer> knights;
    protected List<Integer> workers;
    protected List<Integer> mages;
    protected List<Integer> healers;
    
    protected Set<Integer> livingUnits;
    
    protected Queue<ManufactureRequest> buildQueue;
    
    protected Map<Integer, UnitHandler> handlerManager;

    public PlanetController(GameController gc, PathMaster pm, Random rng) {
        this.gc = gc;
        this.rng = rng;
        this.pm = pm;
        
        this.factories = new LinkedList<Integer>();
        this.rockets = new LinkedList<Integer>();
        this.rangers = new LinkedList<Integer>();
        this.knights = new LinkedList<Integer>();
        this.workers = new LinkedList<Integer>();
        this.mages = new LinkedList<Integer>();
        this.healers = new LinkedList<Integer>();
        
        this.livingUnits = new HashSet<Integer>();
        
        this.buildQueue = new LinkedList<ManufactureRequest>();
        
        this.handlerManager = new HashMap<Integer, UnitHandler>();
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