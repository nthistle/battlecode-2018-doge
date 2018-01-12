import bc.*;
import java.util.*;

public class MovementHandler extends UnitHandler {
	public Set<String> visited;
	public MovementHandler(GameController gc, int id, Random rng, Set<String> visited) {
		super(gc, id, rng);
		this.visited = visited;
	}
	
	public void takeTurn() {
		return;
	}
	
	public boolean recurMove() {
		visited.add(gc.unit(this.id).toJson());
		for(Direction c : Direction.values()) {
			if(gc.canMove(this.id, c)) {
				gc.moveRobot(this.id, c);
				return true;
			}
		}
		MapLocation myLocation = gc.unit(this.id).location().mapLocation(); 
		VecUnit adjacent = gc.senseNearbyUnitsByTeam(myLocation, 2, gc.team());
		for(int i = 0; i < adjacent.size(); i++) {
			if(visited.contains(adjacent.get(i).toJson())) continue;
			MapLocation itsLocation = adjacent.get(i).location().mapLocation();
			MovementHandler move = new MovementHandler(this.gc, gc.senseUnitAtLocation(itsLocation).id(), this.rng, this.visited);
			if(move.recurMove()) return true;
		}
		return false;
	}
}