import bc.*;
import java.util.Random;

public class FactoryHandler extends UnitHandler {

	public static int rangersCreated = 0;
	public static RangerSwarm rangerSwarm = null;

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
        if(this.rangersCreated < 8) {
        	if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
            	gc.produceRobot(this.id, UnitType.Ranger);
        	}
        	this.rangersCreated++;
        } else {
        	((EarthController) parent).getSwarm().add(this.rangerSwarm);
        	this.rangerSwarm.setSwarmLeader(new MapLocation(Planet.Earth, 10, 10));
        }
        VecUnitID garrison = unit.structureGarrison();
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