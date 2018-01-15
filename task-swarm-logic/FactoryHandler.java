import bc.*;
import java.util.Random;

public class FactoryHandler extends UnitHandler {

	/*
	public static int rangersCreated = 0;
	public static RangerSwarm rangerSwarm = null;
	public static int swarmsCreated = 0;
	*/

    public FactoryHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        if(this.rangerSwarm == null) {
        	this.rangerSwarm = new RangerSwarm(gc);
        }
    }

    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {

    	if(this.rangerSwarm.getUnits().size() == this.rangerSwarm.MIN_SWARM_SIZE) {
    		this.swarmsCreated += 1;
    		((EarthController) parent).getSwarm().add(this.rangerSwarm);
    		switch(this.swarmsCreated) {
    			case 1:
    				this.rangerSwarm.setSwarmLeader(new MapLocation(Planet.Earth, 10, 10));
    				this.rangerSwarm = new RangerSwarm(gc);
    				break;
    			case 2:
    				this.rangerSwarm.setSwarmLeader(new MapLocation(Planet.Earth, 10, 20));
    				break;
    			default:
    				break;
    		}
    		
    	}

    	if(unit.structureIsBuilt() != 0) {
	    	//System.out.println("Rangers created: " + this.rangersCreated);
	        if(this.rangersCreated < 9) {
	        	if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
	        		System.out.println("Ranger produced");
	            	gc.produceRobot(this.id, UnitType.Ranger);
	            	this.rangersCreated++;
	        	}	
	        } else if(this.rangersCreated >= 9 && this.rangersCreated < 20) {
	        	if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
	        		System.out.println("Ranger produced");
	            	gc.produceRobot(this.id, UnitType.Ranger);
	            	this.rangersCreated++;
	        	}
	        }
	        VecUnitID garrison = unit.structureGarrison();
	        //System.out.println("Garrison size: " + garrison.size());
	        if(garrison.size() > 0) {
	            for(int i = 0; i < 8; i ++) {
	                Direction unloadDir = Utils.getRandomDirection(Direction.values(), this.rng);
	                if(gc.canUnload(this.id, unloadDir)) {
	                    gc.unload(this.id, unloadDir);
	                    rangerSwarm.addUnit(gc.senseUnitAtLocation(unit.location().mapLocation().add(unloadDir)).id());
	                }
	            }
	        }
    	}
    }
}