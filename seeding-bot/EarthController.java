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
    
    public PlanetMap map;
    public Team enemyTeam;

    public VecUnit units;
    public HashMap<UnitType, Integer> robotCount;    

    public HashMap<String, Long> moneyCount;
    public LinkedList<MapLocation> moneyLocations;    

    public HashMap<Integer,UnitHandler> myHandler;

    public void control() {
    
        System.out.println("Earth Controller iniatied");

        globalValues();
        initialMoneyInfo();

        myHandler = new HashMap<Integer,UnitHandler>();

        while (true) {
        
            System.out.println("Round #" + gc.round() + ", " + gc.getTimeLeftMs());

            update();
            
            for (int i = 0; i < units.size(); i++) {
                Unit unit = units.get(i);
                myHandler.get(unit.id()).takeTurn(unit);
            }

            gc.nextTurn();
        }
    }

    // initialize global values
    private void globalValues() {
        enemyTeam = Utils.getOtherTeam(gc.team());
        map = gc.startingMap(Planet.Earth);        
    }

    // store initial karbonite locations and values
    private void initialMoneyInfo() {
        moneyCount = new HashMap<String, Long>();
        moneyLocations = new LinkedList<MapLocation>();
        for (int i = 0; i < map.getHeight(); i++) {
            for (int j = 0; j < map.getWidth(); j++) {
                MapLocation tempLocation = new MapLocation(Planet.Earth, j, i);
                moneyCount.put(tempLocation.toJson(), map.initialKarboniteAt(tempLocation));
                moneyLocations.add(tempLocation);
            }
        }
    }

    // round by round independent updates
    // current: create handlers and update unit counts
    private void update() {
        units = gc.myUnits();
        robotCount = new HashMap<UnitType, Integer>();
        for(int i = 0; i < units.size(); i++) {
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

    public Planet getPlanet() {
        return Planet.Earth;
    }
}