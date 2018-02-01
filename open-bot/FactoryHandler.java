import bc.*;
import java.util.EnumMap;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class FactoryHandler extends UnitHandler {

    public static final int MAX_BQUEUE_SIZE = 7;
    public static final int IDEAL_BQUEUE_SIZE = 3;

	protected Deque<UnitType> buildQueue;
	protected EarthController myParent;
    protected Map<UnitType, Integer> robotCount;

    public FactoryHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        this.myParent = (EarthController) parent;
        buildQueue = new LinkedList<UnitType>();
        robotCount = new EnumMap<UnitType, Integer>(UnitType.class);
        for(UnitType ut : UnitType.values()) {
            robotCount.put(ut, 0);
        }
    }

    public UnitType peekBuildQueue() {
        return buildQueue.peek();
    }

    public boolean forceAddPriorityBuildQueue(UnitType ut) {
        robotCount.put(ut, robotCount.get(ut) + 1);
        return this.buildQueue.offerFirst(ut);
    }

    public boolean addToBuildQueue(UnitType ut) {
    	if(buildQueue.size() >= MAX_BQUEUE_SIZE)
    		return false;
        robotCount.put(ut, robotCount.get(ut) + 1);
    	return buildQueue.offer(ut);
    }

    public int getBuildQueueSize() {
    	return buildQueue.size();
    }

    public void clearBuildQueue() {
        for(UnitType ut : UnitType.values()) {
            robotCount.put(ut, 0);
        }
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

    	if(gc.canProduceRobot(this.id, buildQueue.peek()) && usableKarbonite > bc.bcUnitTypeFactoryCost(buildQueue.peek())) {
            if (buildQueue.peek() == UnitType.Worker && myParent.queuedWorkers > 0) {
                myParent.queuedWorkers--;
            }
    		// now we can build this guy
            robotCount.put(buildQueue.peek(), robotCount.get(buildQueue.peek()) - 1);
    		gc.produceRobot(this.id, buildQueue.poll());
    	}
    }

    public void handleDeath() {}
}