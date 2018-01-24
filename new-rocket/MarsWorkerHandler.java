import bc.*;
import java.util.*;

public class MarsWorkerHandler extends UnitHandler {

	private int curReplicateCooldown = 0;
	private static final int REPLICATE_COOLDOWN = 10;
	
    public MarsWorkerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
    	//Hiearchy of action:
    	//0. RUN AWAY FROM BAD THINGS
    	//1. Replicate
    	//2. Mine
    	//3. Move
    	
    	boolean didStuff = false;
    	
    	//run like a bitch
    	RocketLandingInfo rli = gc.rocketLandings();
    	List<MapLocation> bad = new LinkedList<MapLocation>();
    	//peep next ten rounds
    	for(int i = (int)gc.round(); i < (int)gc.round() + 10; i++) {
    		VecRocketLanding landings = rli.landingsOn(i);
    		for(int j = 0; j < landings.size(); j++) {
    			bad.add(landings.get(j).getDestination());
    		}
    	}
    	MapLocation myLocation = unit.location().mapLocation();
    	
    	MapLocation badPlace = null;
    	for(MapLocation loc : bad) {
    		if(myLocation.distanceSquaredTo(loc) <= 2) badPlace = loc; //oh shit run away
    	}
    	if(badPlace != null) {
    		Utils.tryMoveRotate(gc, this.id, badPlace.directionTo(myLocation)); //go away
    	}
    	
    	//replicate
    	if((gc.karbonite() > 250 && gc.round() < 750) || (gc.round() >= 750 && gc.karbonite() >= 30)) { //before, seldom replicate. After, spam that shit. 
    		for(Direction c : Direction.values()) {
    			if(gc.canReplicate(this.id, c)) {
    				gc.replicate(this.id, c);
    				didStuff |= true;
    				break;
    			}
    		}
    	}
    	    	
    	//mine
    	for (Direction c : Direction.values()) {
    		if(gc.canHarvest(this.id, c)) {
    			gc.harvest(this.id, c);
    			didStuff |= true;
    		}
    	}
    	
    	if(didStuff) return;
    	
    	//random move
    	if(gc.isMoveReady(this.id)) {
            for(int i = 0; i < 5; i ++) {
                Direction moveDir = Utils.getRandomDirection(Direction.values(), this.rng);
                if(gc.canMove(this.id, moveDir)) {
                    gc.moveRobot(this.id, moveDir);
                    break;
                }
            }
        }   	
    	
    	
    }
}