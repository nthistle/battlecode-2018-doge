import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

public class EarthController extends PlanetController
{
    public HashMap<Integer,UnitHandler> myHandler = new HashMap<Integer,UnitHandler>();
    public HashMap<Integer, MapLocation> targets = new HashMap<Integer, MapLocation>();

    //testing purposes
    int beginningWorkers = 1;

    public EarthController(GameController gc, PathMaster pm, Random rng) {
        super(gc, pm, rng);
    }
    
    public void control() {
    
        System.out.println("Earth Controller iniatied");

        MiningMaster mm = new MiningMaster(this);
        mm.generate();
    
        //mm.printKarboniteMap();

        Unit firstWorker = gc.myUnits().get(0);



        while (true) {
        
            System.out.println("Round #"+gc.round());

            VecUnit units = gc.myUnits();
            System.out.println("We have " + units.size() + " units");
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                
                    UnitHandler newHandler = null;
                    
                    switch(unit.unitType()) {
                        case Worker:
                            newHandler = new MiningWorkerHandler(this, gc, unit.id(), rng, mm);
                            System.out.println("Created a new MiningWorkerHandler");
                            break;
                        default:
                            break;
                    }
                    
                    myHandler.put(unit.id(), newHandler);
                }
                if(myHandler.get(unit.id()) instanceof MiningWorkerHandler) {
                    mm.assignTarget((MiningWorkerHandler) myHandler.get(unit.id()));
                }
                myHandler.get(unit.id()).takeTurn(unit);             
            }

            gc.nextTurn();
        }
    }
    
    public Planet getPlanet() {
        return Planet.Earth;
    }
}