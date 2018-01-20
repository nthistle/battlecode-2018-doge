import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.TreeMap;

public class EarthController extends PlanetController
{
    public EarthController(GameController gc, PathMaster pm, Random rng) {
        super(gc, pm, rng);
    }
    
    public void control() {
    
        System.out.println("Earth Controller iniatied");
    
        HashMap<Integer,UnitHandler> myHandler = new HashMap<Integer,UnitHandler>();

        while (true) {
        
            System.out.println("Round #"+gc.round());
            
            VecUnit units = gc.myUnits();
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                
                    UnitHandler newHandler = null;
                    
                    switch(unit.unitType()) {
                        case Factory:
                            newHandler = new FactoryHandler(this, gc, unit.id(), rng);
                            break;
                        case Knight:
                            newHandler = new KnightHandler(this, gc, unit.id(), rng);
                            break;
                        case Ranger:
                            newHandler = new RangerHandler(this, gc, unit.id(), rng);
                            break;
                        case Worker:
                            newHandler = new WorkerHandler(this, gc, unit.id(), rng);
                            break;
                        default:
                            break;
                    }
                    
                    myHandler.put(unit.id(), newHandler);
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