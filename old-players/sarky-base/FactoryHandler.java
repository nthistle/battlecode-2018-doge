import bc.*;
import java.util.*;

public class FactoryHandler extends UnitHandler {

    public FactoryHandler(GameController gc, int id, Random rng) {
        super(gc, id, rng);
    }
    
    public void takeTurn() {
    	this.takeTurn(gc.unit(this.id));
    }
    
    public void takeTurn(Unit unit) {
    	double troopType = rng.nextDouble();
    	if(troopType <= 0.2 && gc.canProduceRobot(this.id, UnitType.Worker)) {
    		gc.produceRobot(this.id, UnitType.Worker);
    	}
    	else if(troopType <= 0.7 && gc.canProduceRobot(this.id, UnitType.Knight)) {
    		gc.produceRobot(this.id, UnitType.Knight);
    	}
    	else if(troopType > 0.7 && gc.canProduceRobot(this.id, UnitType.Ranger)) {
    		gc.produceRobot(this.id, UnitType.Ranger);
    	}
        VecUnitID garrison = unit.structureGarrison();
        if(garrison.size() > 0) {
        	for(int i = 0; i < garrison.size(); i++) {
        		//if empty adjacent space, unload
        		boolean placed = false;
        		for(Direction c : Direction.values()) {
        			if(gc.canUnload(this.id, c)) {
        				gc.unload(this.id, c);
        				placed = true;
        				break;
        			}
        		}
        		if(placed) continue;
        		MapLocation myLocation = unit.location().mapLocation(); 
        		VecUnit adjacent = gc.senseNearbyUnitsByTeam(myLocation, 2, gc.team());
        		for(int j = 0; j < adjacent.size(); j++) {
        			MapLocation itsLocation = adjacent.get(j).location().mapLocation();
        			Direction itsDirection = myLocation.directionTo(adjacent.get(j).location().mapLocation());
        			Set<String> visited = new HashSet<String>();
        			visited.add(gc.unit(this.id).toJson());
        			MovementHandler move = new MovementHandler(this.gc, gc.senseUnitAtLocation(itsLocation).id(), this.rng, visited);
        			placed = move.recurMove();
        			if(placed) {
        				gc.unload(this.id, itsDirection);
        				break;
        			}
        		}
        	}
        }
    }
}