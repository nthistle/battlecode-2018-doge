import bc.*;
import java.util.*;

public class MarsRocketHandler extends UnitHandler {
    public MarsRocketHandler(PlanetController parent, GameController gc, int id, Random rng){
        super(parent, gc, id, rng);
        //like i said, really dumb init data
    }
    
    public void takeTurn() {
    	this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
    	if(gc.unit(this.id).structureGarrison().size() > 0) {
    		this.unload();
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