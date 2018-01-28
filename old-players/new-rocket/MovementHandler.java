import bc.*;
import java.util.*;

public class MovementHandler extends UnitHandler {
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
    		System.out.println(gc.unit(this.id));
    		if(gc.canMove(this.id, c) && gc.isMoveReady(this.id)) {
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