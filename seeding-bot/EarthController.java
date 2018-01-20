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
    
    public PlanetMap earthMap;

    public Team enemyTeam;

    public HashMap<UnitType, Integer> robotCount;    
    public HashMap<String, Long> moneyCount;
    public LinkedList<MapLocation> moneyLocations;
    

    public HashMap<Integer,UnitHandler> myHandler;

    public void control() {
    
        System.out.println("Earth Controller iniatied");
    
        myHandler = new HashMap<Integer,UnitHandler>();
        enemyTeam = Utils.getOtherTeam(gc.team());

        earthMap = gc.startingMap(Planet.Earth);

        moneyCount = new HashMap<String, Long>();
        moneyLocations = new LinkedList<MapLocation>();
        for (int i = 0; i < earthMap.getHeight(); i++) {
            for (int j = 0; j < earthMap.getWidth(); j++) {
                MapLocation tempLocation = new MapLocation(Planet.Earth, j, i);
                moneyCount.put(tempLocation.toJson(), earthMap.initialKarboniteAt(tempLocation));
                moneyLocations.add(tempLocation);
            }
        }

        while (true) {
        
            System.out.println("Round #" + gc.round() + ", " + gc.getTimeLeftMs());
            robotCount = new HashMap<UnitType, Integer>(); 
                        
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
                incrementRobotCount(unit.unitType());                

            }
            for (int i = 0; i < units.size(); i++) {
                Unit unit = units.get(i);
                myHandler.get(unit.id()).takeTurn(unit);
            }
            gc.nextTurn();
        }
    }
    
    public int getRobotCount(UnitType type) {
        if (!robotCount.containsKey(type)) {
            return 0;
        }
        return robotCount.get(type);
    }

    public void incrementRobotCount(UnitType type) {
        robotCount.put(type, getRobotCount(type) + 1);
    }
    
    private void initialMoneyInfo() {
        moneyCount = new HashMap<String, Long>();
        moneyLocations = new LinkedList<MapLocation>();
        for (int i = 0; i < earthMap.getHeight(); i++) {
            for (int j = 0; j < earthMap.getWidth(); j++) {
                MapLocation tempLocation = new MapLocation(Planet.Earth, j, i);
                moneyCount.put(tempLocation.toJson(), earthMap.initialKarboniteAt(tempLocation));
                moneyLocations.add(tempLocation);
            }
        }
    }

    public Planet getPlanet() {
        return Planet.Earth;
    }
}