import bc.*;
import java.util.Random;

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
    	//1. Replicate
    	//2. Mine
    	//3. Move
    	
    	boolean didStuff = false;
    	curReplicateCooldown--;
    	
    	//replicate
    	if(curReplicateCooldown <= 0) {
    		for(Direction c : Direction.values()) {
    			if(gc.canReplicate(this.id, c)) {
    				gc.replicate(this.id, c);
    				this.curReplicateCooldown += REPLICATE_COOLDOWN;
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
    	
    	//move
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