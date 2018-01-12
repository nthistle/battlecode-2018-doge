import bc.*;
import java.util.Random;
import java.util.Set;

public class MovementHandler {
	public Set<String> visited;
	public void MovementHandler(GameController gc, int id, Random rng, Set<String> visited) {
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
				gc.move(this.id, c);
				return true;
			}
		}
		VecUnit adjacent = gc.senseNearbyUnitsByTeam(myLocation, 2, gc.team());
		for(int i = 0; i < adjacent.size(); i++) {
			if(nearby.contains(gc.unit(this.id).toJson())) continue;
			MapLocation itsLocation = nearby.get(i).location().mapLocation();
			MovementHandler move = new MovementHandler(gc, gc.senseUnitAtLocation(itsLocation).id(), rng);
			if(move.recurMove()) return true;
		}
		return false;
	}
}