import bc.*;
import java.util.*;

public class WorkerHandler extends UnitHandler {

	private int curFactoryCooldown = 0;
	private static final int FACTORY_COOLDOWN = 50;
	private boolean builtRocket = false;
	
    public WorkerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
    	//Hiearchy of action:
    	//0. SET MOVEMENT
    	//1. Build
    	//2. Blueprint
    	//3. Mine
    	//4. Random move
    	
    	
    	/* set move */
    	MapLocation myLocation = unit.location().mapLocation();
    	System.out.println(myLocation);
    	System.out.println(this.rocketBound);
    	System.out.println(this.target);
    	if(this.rocketBound && this.target != null) {
    		Direction dir = parent.pm.getPathField(this.target).getDirectionAtPoint(unit.location().mapLocation().getX(), unit.location().mapLocation().getY());
	    	Utils.tryMoveWiggleRecur(gc, this.id, dir, null);
    		return;
    	}
    	
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
    		if(gc.canBlueprint(this.id, UnitType.Rocket, c) && curFactoryCooldown <= 0 && !builtRocket) {
    			gc.blueprint(this.id, UnitType.Rocket, c);
    			this.curFactoryCooldown += FACTORY_COOLDOWN;
    			MapLocation rocketLoc = unit.location().mapLocation().add(c);
    			parent.rockets.add(gc.senseUnitAtLocation(rocketLoc).id());
    			parent.livingUnits.add(gc.senseUnitAtLocation(rocketLoc).id());
    			parent.handlerManager.put(gc.senseUnitAtLocation(rocketLoc).id(), new RocketHandler(parent, gc, gc.senseUnitAtLocation(rocketLoc).id(), rng, ((EarthController) parent).launchLogic, RocketHandler.FIRST_CONTACT_CREW));
    			builtRocket = true;
    			return;
    		}
    		else if(gc.canBlueprint(this.id, UnitType.Factory, c) && curFactoryCooldown <= 0) {
    			gc.blueprint(this.id, UnitType.Factory, c);
    			this.curFactoryCooldown += FACTORY_COOLDOWN; MapLocation rocketLoc = unit.location().mapLocation().add(c);
    			MapLocation factoryLoc = unit.location().mapLocation().add(c);
    			parent.factories.add(gc.senseUnitAtLocation(factoryLoc).id());
    			parent.livingUnits.add(gc.senseUnitAtLocation(factoryLoc).id());
    			parent.handlerManager.put(gc.senseUnitAtLocation(factoryLoc).id(), new FactoryHandler(parent, gc, gc.senseUnitAtLocation(factoryLoc).id(), rng));
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