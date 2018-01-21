import bc.*;
import java.util.*;

public class FactoryHandler extends UnitHandler {
	private Map<String, Queue<ManufactureRequest>> workInProgress;
    public FactoryHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        this.workInProgress = new HashMap<String, Queue<ManufactureRequest>>();
        for(UnitType ut : UnitType.values()) {
        	this.workInProgress.put(ut.toString(), new LinkedList<ManufactureRequest>());
        }
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {

        /*
        int shouldProduce = this.rng.nextInt(150);
        if(shouldProduce == 0) {
            if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
                gc.produceRobot(this.id, UnitType.Ranger);
            }
        } else if(shouldProduce == 1) {
            if(gc.canProduceRobot(this.id, UnitType.Knight)) {
                gc.produceRobot(this.id, UnitType.Knight);
            }
        }
        */
        if(unit.structureIsBuilt() != 0) {
        	//check if robot can be built
        	if(parent.buildQueue.size() > 0) {
        		ManufactureRequest mr = parent.buildQueue.peek();
        		if(gc.canProduceRobot(this.id, mr.request)) {
        			mr = parent.buildQueue.remove();
        			workInProgress.get(mr.request.toString()).add(mr);
        			gc.produceRobot(this.id, mr.request);
        		}
        	}
        	//take out any finished robots and assign them a job
        	if(unit.structureGarrison().size() > 0){
        		for(Direction c : Direction.values()) {
        			if(gc.canUnload(this.id, c)) {
        				gc.unload(this.id, c);
        				Unit unloadee = gc.senseUnitAtLocation(unit.location().mapLocation().add(c));
        				System.out.println(unloadee);
        				ManufactureRequest mr = this.workInProgress.get(unloadee.unitType().toString()).remove();
        				UnitHandler handler = null;
        				switch(unloadee.unitType()) {
        					case Worker:
        						parent.workers.add(unloadee.id());
        						parent.livingUnits.add(unloadee.id());
        						handler = new WorkerHandler(parent, gc, unloadee.id(), rng);
        						handler.setTarget(mr.target);
        						handler.makeRocketBound(mr.rocketBound);
        						break;
        					case Ranger:
        						parent.rangers.add(unloadee.id());
        						parent.livingUnits.add(unloadee.id());
        						handler = new RangerHandler(parent, gc, unloadee.id(), rng);
        						handler.setTarget(mr.target);
        						handler.makeRocketBound(mr.rocketBound);
        						break;
        				}
        				parent.handlerManager.put(unloadee.id(), handler);
        			}
        		}
        	}
        }
    }
}