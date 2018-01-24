import bc.*;
import java.util.*;

public class RocketHandler extends UnitHandler {
	//static frequency tables
	public static final double[] RANGER_CREW = new double[] {0, 1, 0, 0, 0};
	public static final double[] KNIGHT_CREW = new double[] {0, 0, 1, 0, 0};
	public static final double[] TANKY_CREW = new double[] {0, 0.5, 0.5, 0, 0};
	public static final double[] FIRST_CONTACT_CREW = new double[] {0.5, 0.5, 0, 0, 0};
	public static final double[] ARTISTIC_CREW = new double[] {0, 0, 0, 0.5, 0.5}; //send in the wierder, more niche troops
	
	private Map<UnitType, Integer> targetManifest;
    private Map<UnitType, Integer> stillNeeded;
	private MapLocation dest;
	private LaunchingLogicHandler llh;
    public final boolean firstContact; 
	
	/**
	 * generate a rocket handler for a rocket
	 * 
	 * @param llh a reference to the LaunchingLogicHandler for EarthController
	 * @param manifest one of the final static double arrays that describes a crew manifest for a rocket
	 */
    public RocketHandler(PlanetController parent, GameController gc, int id, Random rng, LaunchingLogicHandler llh, double[] manifest) {
        super(parent, gc, id, rng);
        
        this.firstContact = manifest[0] > 0;
        
        //parse the manifestj
        this.targetManifest = new HashMap<UnitType, Integer>();
        this.stillNeeded = new HashMap<UnitType, Integer>();

        this.targetManifest.put(UnitType.Worker, (int) (gc.unit(this.id).structureMaxCapacity() * manifest[0]));
        this.targetManifest.put(UnitType.Ranger, (int) (gc.unit(this.id).structureMaxCapacity() * manifest[1]));
        this.targetManifest.put(UnitType.Knight, (int) (gc.unit(this.id).structureMaxCapacity() * manifest[2]));
        this.targetManifest.put(UnitType.Mage  , (int) (gc.unit(this.id).structureMaxCapacity() * manifest[3]));
        this.targetManifest.put(UnitType.Healer, (int) (gc.unit(this.id).structureMaxCapacity() * manifest[4]));

        for(UnitType key : this.targetManifest.keySet()) {
            this.stillNeeded.put(key, this.targetManifest.get(key));
        }
        //basically, the above transforms one of the static final double troop frequency tables into an actual manifest
        //when all the terms in the manifest go to zero, the rocket is ready to fire up and blast off!
        
        this.dest = gc.unit(this.id).location().mapLocation();
        this.llh = llh;

        this.parent.pm.getAndCachePathField(this.dest);
        // this path field gets un-cached on takeoff
        // AstronautHandlers will reassign themselves naturally if they see it uncached (i.e. it grabs another unit of the
        // same type instead of the one that was going to it)
    }
    
    public void takeTurn() {
    	this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
    	if(unit.structureIsBuilt() != 0) {
    		this.load();
    		this.makeRequests();
            this.setDestination(llh.optimalLandingLocation(this.firstContact));
    		// System.out.println("Dest: " + this.getDestination());
    		if(this.shouldLaunch() && gc.canLaunchRocket(this.id, this.dest)) {
    			this.blastOff();
    			llh.addUsedMapLocation(this.getDestination());
    		}
    	}
    }
    
    // NOT WORKING
    public void makeRequests() {
    	/*for(String type : this.wantedTroops) {
    		if(this.canRequest.get(type)) {
        		parent.buildQueue.add(new ManufactureRequest(UnitType.valueOf(type), gc.unit(this.id).location().mapLocation(), true));
        		this.canRequest.put(type, false);
    		}
    	}*/
    }
    
    /**
     * launch this rocket
     * 
     * @return RocketLanding object for this rocket, if succesful. Otherwise, null. 
     */
    public void blastOff() {
    	if(!gc.canLaunchRocket(this.id, this.dest)) return;
    	else {
    		this.emergencyLoad();
    		gc.launchRocket(this.id, this.dest);
    	}
    }
    
    public void setLogicHandler(LaunchingLogicHandler llh) {
    	this.llh = llh;
    }

    public Set<String> getWantedTroops() {
    	return this.wantedTroops;
    }
    
    public Map<String, Integer> getCurrentManifest() {
    	return this.targetManifest;
    }
    
    public void setDestination(MapLocation dest) {
    	this.dest = dest;
    }
    
    public MapLocation getDestination() {
    	return this.dest;
    }
    
    public boolean shouldLaunch() {
    	// System.out.println(this.llh.optimalLaunchingTime());
    	// System.out.println(gc.round());
    	if(this.isLoaded() && this.llh.optimalLaunchingTime() == gc.round()) return true;
    	else if(gc.unit(this.id).health() <= 150) return true;
    	//TODO: expand list of cases to include damage nearby, etc. 
    	else return false;
    }
    
    /**
     * Checks to see if the rocket is fully stocked with the intended swarm
     * 
     * @return true if fully stocked, false if not
     */
    public boolean isLoaded() {
    	// System.out.println("Wanted troops: " + this.wantedTroops);
    	// System.out.println("Wanted troops size: " + this.wantedTroops.size());
        for(UnitType key : this.targetManifest.keySet()) {
            if(this.stillNeeded.get(key) != 0) return false;
        }
        return true;
    }
    
    /**
     * attempts to load the given troop
     * 
     * @return true if succesful, false if not
     */
    public boolean loadTroop(int unitID) {
    	if(!gc.canLoad(this.id, unitID)) return false;
    	else {
    		gc.load(this.id, unitID);
    		return true;
    	}
    }
    
    /**
     * only loads wanted troops 
     */
    public void load() {
  //   	System.out.println("Loading...");
		// System.out.println("what i want: " + this.targetManifest);
    	MapLocation myLocation = gc.unit(this.id).location().mapLocation();
    	VecUnit adjacent = gc.senseNearbyUnitsByTeam(myLocation, 2, gc.team());
  //   	System.out.println(this.wantedTroops);
        Unit adj;
    	for(int i = 0; i < adjacent.size(); i++) {
            adj = adjacent.get(i);
    		if(this.stillNeeded.contains(adj.unitType())) {
                if(parent.myHandler.get(adj.id()) instanceof MiningWorkerHandler)
                    continue;
  //   			System.out.println("Loading " + adjacent.get(i).id());
    			if(this.loadTroop(adj.id())) {
                    this.stillNeeded.put(adj.unitType(), this.stillNeeded.get(adj.unitType()) - 1);
    			}
    		}
    	}
    }
    
    /**
     * loads all adjacent troops until rocket is full or all are loaded
     */
    public void emergencyLoad() {
    	MapLocation myLocation = gc.unit(this.id).location().mapLocation();
    	VecUnit adjacent = gc.senseNearbyUnitsByTeam(myLocation, 2, gc.team());
    	for(int i = 0; i < adjacent.size(); i++) {
    		if(gc.unit(this.id).structureGarrison().size() >= gc.unit(this.id).structureMaxCapacity()) break;
    		else this.loadTroop(adjacent.get(i).id());
    	}
    }
    
   
    
    
}