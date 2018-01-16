import bc.*;
import java.util.*;

public class RocketHandler extends UnitHandler {
	private Swarm crew;
	private MapLocation dest;
	private LaunchingLogicHandler llh;
    public RocketHandler(PlanetController parent, GameController gc, int id, Random rng, LaunchingLogicHandler llh) {
        super(parent, gc, id, rng);
        //dummy init data
        //be sure to override when the time is right
        this.crew = null;
        this.dest = gc.unit(this.id).location().mapLocation();
        this.llh = llh;
        //like i said, really dumb init data
    }
    
    public void takeTurn() {
    	this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
    	this.load();
    	this.setDestination(llh.optimalLandingLocation());
    	if(this.shouldLaunch()) {
    		this.blastOff();
    		llh.addUsedMapLocation(this.getDestination());
    	}
    }
    
    /**
     * launch this rocket
     * 
     * @return RocketLanding object for this rocket, if succesful. Otherwise, null. 
     */
    public RocketLanding blastOff() {
    	if(!gc.canLaunchRocket(this.id, this.dest)) return null;
    	else {
    		this.emergencyLoad();
    		gc.launchRocket(this.id, this.dest);
    		VecRocketLanding vec = gc.rocketLandings().landingsOn(gc.round() + gc.currentDurationOfFlight());
    		for(int i = 0; i < vec.size(); i++) {
    			if(vec.get(i).getRocket_id() == this.id) return vec.get(i);
    		}
    		return null; //shouldn't get here. Please. 
    	}
    }
    
    
    public void setSwarm(Swarm swarm) {
    	this.crew = swarm;
    }
    
    public Swarm getSwarm() {
    	return this.crew;
    }
    
    /**
     * set the rocket's destination
     * 
     * @return true if assigning is valid and succesful, false otherwise
     */
    public boolean setDestination(MapLocation dest) {
    	if(gc.canLaunchRocket(this.id, this.dest)) {
    		this.dest = dest;
    		return true;
    	}
    	else return false;
    }
    
    public MapLocation getDestination() {
    	return this.dest;
    }
    
    public boolean shouldLaunch() {
    	return this.isLoaded() && this.llh.optimalLaunchingTime() == gc.round() && gc.canLaunchRocket(this.id, this.dest)//good situation
    			|| gc.unit(this.id).health() < 150 && gc.canLaunchRocket(this.id, this.dest);//bad situation
    }
    
    /**
     * Checks to see if the rocket is fully stocked with the intended swarm
     * 
     * @return true if fully stocked, false if not
     */
    public boolean isLoaded() {
    	if(this.crew == null) return false;
    	VecUnitID garrison = gc.unit(this.id).structureGarrison();
    	if(garrison.size() != this.crew.getUnits().size()) return false;
    	for(int i = 0; i < garrison.size(); i++) {
    		if(this.crew.getUnits().contains(garrison.get(i))) continue;
    		else return false;
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
     * loads swarm troops exclusively
     */
    public void load() {
    	if(this.crew == null) return;
    	MapLocation myLocation = gc.unit(this.id).location().mapLocation();
    	VecUnit adjacent = gc.senseNearbyUnitsByTeam(myLocation, 2, gc.team());
    	for(int i = 0; i < adjacent.size(); i++) {
    		if(this.crew.getUnits().contains(adjacent.get(i).id())) this.loadTroop(adjacent.get(i).id());
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
    
    /**
     * Attempts to unload the troop in the neccesary direction
     * 
     * @return true if succesful, false otherwise
     */
    public boolean unloadTroop(int unitID, Direction dir) {
    	if(!gc.canUnload(this.id, dir)) return false;
    	else {
    		gc.unload(this.id, dir);
    		return true;
    	}
    }
    
    /**
     * Attempts to completely empty the rocket
     * 
     * @return true if succesful, false otherwise
     */
    public boolean unload() {
    	boolean ret = true;
    	VecUnitID garrison = gc.unit(this.id).structureGarrison();
    	if(garrison.size() <= 0) return true;
    	else {
    		for(int i = 0; i < garrison.size(); i++) {
    			boolean placed = false;
    			for(Direction c : Direction.values()) {
        			if(this.unloadTroop(this.id, c)) {
        				placed = true;
        				break;
        			}
        		}
    			if(placed) continue;
    			MapLocation myLocation = gc.unit(id).location().mapLocation(); 
        		VecUnit adjacent = gc.senseNearbyUnitsByTeam(myLocation, 2, gc.team());
        		for(int j = 0; j < adjacent.size(); j++) {
        			MapLocation itsLocation = adjacent.get(j).location().mapLocation();
        			Direction itsDirection = myLocation.directionTo(adjacent.get(j).location().mapLocation());
        			Set<Integer> visited = new HashSet<Integer>();
        			visited.add(this.id);
        			MovementHandler move = new MovementHandler(this.parent, this.gc, gc.senseUnitAtLocation(itsLocation).id(), this.rng, visited);
        			placed = move.recurMove();
        			if(placed) {
        				gc.unload(this.id, itsDirection);
        				break;
        			}
        		}
        		ret &= placed;
    		}
    	}
    	return ret;
    }
   
    
    private class MovementHandler extends UnitHandler {
    	public Set<Integer> visited;
    	public MovementHandler(PlanetController parent, GameController gc, int id, Random rng, Set<Integer> visited) {
            super(parent, gc, id, rng);
            this.visited = visited;
    	}
    	
    	public void takeTurn() {
    		return;
    	}
    	
    	public boolean recurMove() {
    		visited.add(this.id);
    		for(Direction c : Direction.values()) {
    			if(gc.canMove(this.id, c)) {
    				gc.moveRobot(this.id, c);
    				return true;
    			}
    		}
    		MapLocation myLocation = gc.unit(this.id).location().mapLocation(); 
    		VecUnit adjacent = gc.senseNearbyUnitsByTeam(myLocation, 2, gc.team());
    		for(int i = 0; i < adjacent.size(); i++) {
    			if(visited.contains(adjacent.get(i).id())) continue;
    			MapLocation itsLocation = adjacent.get(i).location().mapLocation();
    			MovementHandler move = new MovementHandler(this.parent, this.gc, gc.senseUnitAtLocation(itsLocation).id(), this.rng, this.visited);
    			if(move.recurMove()) return true;
    		}
    		return false;
    	}
    }
    
}