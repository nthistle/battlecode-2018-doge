import bc.*;
import java.util.Random;

public class WorkerHandler extends UnitHandler {

	private int curFactoryCooldown = 0;
	private static final int FACTORY_COOLDOWN = 10;
	
    public WorkerHandler(GameController gc, int id, Random rng) {
        super(gc, id, rng);
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
    	//Hiearchy of action:
    	//1. Build
    	//2. Blueprint
    	//3. Mine
    	//4. Move
    	
    	/* build */
    	VecUnit nearby = gc.senseNearbyUnits(unit.location().mapLocation(), 2);
    	for(int i = 0; i < nearby.size(); i++) {
    		if(gc.canBuild(this.id, nearby.get(i).id())) {
    			gc.build(this.id, nearby.get(i).id());
    			return;
    		}
    	}
    	
    	if(curFactoryCooldown > 0) curFactoryCooldown--;
    	
    	/* blueprint */ 
    	for (Direction c : Direction.values()) {
    		if(gc.canBlueprint(this.id, UnitType.Factory, c) && curFactoryCooldown <= 0) {
    			gc.blueprint(this.id, UnitType.Factory, c);
    			this.curFactoryCooldown += FACTORY_COOLDOWN; 
    			return;
    		}
    	}
   
    	
    	/* mine */ 
    	for (Direction c : Direction.values()) {
    		if(gc.canHarvest(this.id, c)) {
    			gc.harvest(this.id, c);
    			return;
    		}
    	}
    	
    	/* move */ 
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