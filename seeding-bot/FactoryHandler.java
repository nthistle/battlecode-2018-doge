import bc.*;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;

public class FactoryHandler extends UnitHandler {

	public static final int MAX_BQUEUE_SIZE = 5;

	protected Queue<UnitType> buildQueue;
	protected EarthController myParent;

    public FactoryHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        this.myParent = (EarthController) parent;
        buildQueue = new LinkedList<UnitType>();
    }

    public boolean addToBuildQueue(UnitType ut) {
    	if(buildQueue.size() >= MAX_BQUEUE_SIZE)
    		return false;
    	return buildQueue.offer(ut);
    }

    public int getBuildQueueSize() {
    	return buildQueue.size();
    }

    public void clearBuildQueue() {
    	while(!buildQueue.isEmpty()) buildQueue.remove();
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {

    	if(unit.structureIsBuilt() == 0) return;

    	// unload my garrison
    	if(unit.structureGarrison().size() > 0) {
    		for(Direction dir : Direction.values()) {
    			if(gc.canUnload(this.id, dir)) {
    				gc.unload(this.id, dir);
    			}
    		}
    	}

    	if(buildQueue.size()==0) return;

    	long usableKarbonite = gc.karbonite();
    	// build stuff
    	if(myParent.isSavingForFactory) usableKarbonite -= bc.bcUnitTypeBlueprintCost(UnitType.Factory);
    	if(myParent.isSavingForRocket)  usableKarbonite -= bc.bcUnitTypeBlueprintCost(UnitType.Rocket);

    	if(usableKarbonite > bc.bcUnitTypeFactoryCost(buildQueue.peek())) {
    		// now we can build this guy
    		gc.produceRobot(this.id, buildQueue.poll());
    	}
    }
}