import bc.*;
import java.util.Random;
import java.util.Set;

public class FactoryHandler extends UnitHandler {

    public FactoryHandler(GameController gc, int id, Random rng) {
        super(gc, id, rng);
    }
    
    public void takeTurn() {
    	this.takeTurn(gc.unit(this.id));
    }
    
    public void takeTurn() {
    	int troopType = rng.nextInt();
    	if(troopType > 0.1 && gc.canProduceRobot(this.id, UnitType.Ranger)) {
    		gc.produceRobot(this.id, UnitType.Ranger);
    	}
    	else if(gc.canProduceRobot(this.id, UnitType.Worker)) {
    		gc.produceRobot(this.id, UnitType.Worker);
    	}
        VecUnitID garrison = unit.structureGarrison();
        if(garrison.size() > 0) {
        	for(int i = 0; i < garrison.size(); i++) {
        		//if empty adjacent space, unload
        		boolean placed = false;
        		for(Direction c : Direction.values()) {
        			if(gc.canUnload(this.id, unloadDir)) {
        				gc.unload(this.id, unloadDir);
        				placed = true;
        				break;
        			}
        		}
        		if(placed) continue;
        		MapLocation myLocation = unit.location().mapLocation(); 
        		VecUnit adjacent = gc.senseNearbyUnitsByTeam(myLocation, 2, gc.team());
        		for(int i = 0; i < adjacent.size(); i++) {
        			MapLocation itsLocation = nearby.get(i).location().mapLocation();
        			Set<String> visited = new HashSet<String>();
        			visited.add(gc.unit(this.id).toJson());
        			MovementHandler move = new MovementHandler(this.gc, gc.senseUnitAtLocation(itsLocation).id(), this.rng, visited);
        			placed = move.recurMove();
        			if(placed) {
        				gc.unload(this.id, myLocation.directionTo(nearby.get(i).location().mapLocation());
        				break;
        			}
        		}
        	}
        }
    }
}