import bc.*;
import java.util.*;

public class MarsController extends PlanetController
{
    public MarsController(GameController gc, PathMaster pm, Random rng)  {
        super(gc, pm, rng);
    }
    
    public void control() {
    	
        System.out.println("Mars Controller initiated");
        
        HashMap<Integer, UnitHandler> myHandler = new HashMap<Integer, UnitHandler>();
        
        while (true) {
        	System.out.println("Round #"+gc.round());
            System.out.println("Time left: " + gc.getTimeLeftMs());
        	
            gc.nextTurn();
                        
            VecUnit units = gc.myUnits();
            
            for(int i = 0; i < units.size(); i++) {
            	Unit unit = units.get(i);
            	
            	if(!myHandler.containsKey(unit.id())) {
            		UnitHandler newHandler = null;
            		
            		switch(unit.unitType()) {
            			case Ranger:
            				newHandler = new MarsRangerHandler(this, gc, unit.id(), rng);
            				break;
            			case Worker:
            				newHandler = new MarsWorkerHandler(this, gc, unit.id(), rng);
            				break;
            			case Rocket:
            				newHandler = new MarsRocketHandler(this, gc, unit.id(), rng);
            				break;
            			default:
            				break;
            		}
            		
            		myHandler.put(unit.id(), newHandler);
            	}
            }
            
            for(int id : myHandler.keySet()) {
            	try {
            		myHandler.get(id).takeTurn();
            	}
            	catch(Exception e) {}
            }
            
            gc.nextTurn();
        }
    }
    
    public Planet getPlanet() {
        return Planet.Mars;
    }

    public int getRobotCount(UnitType type) {
        return 0;
    }

    public void incrementRobotCount(UnitType type) {
        return;
    }

    @Override
    public void requestSwarm(int goalSize, MapLocation target, UnitType a) {
        return;
    }
}