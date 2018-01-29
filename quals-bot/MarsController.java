import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.Queue;
import java.util.Collections;

public class MarsController extends PlanetController
{
    public MarsController(GameController gc, Random rng) {
        super(gc, rng);
    }

    public PlanetMap map;
    public Team enemyTeam;
    
    public CommunicationsManager comms;
    
    public void control() {
    
        System.out.println("Mars Controller initiated");

        globalValues();
    
        myHandler = new HashMap<Integer, UnitHandler>();      
        
        comms = new CommunicationsManager(this, gc, rng);

        while (true) {
        
            System.out.println("Round #" + gc.round() + ", (" + gc.getTimeLeftMs() + " ms left)");

            if(gc.getTimeLeftMs() < 1000) {
                System.out.println("TIME POOL LOW! SKIPPING TURN!");
                gc.nextTurn();
                continue;
            }

            System.runFinalization();
            System.gc();            
            
            VecUnit allUnits = gc.units();
            VecUnit units = gc.myUnits();
            
            comms.update();

            refreshTargets(allUnits);

            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                    assignHandler(myHandler, unit);
                }
            }

            takeTurnByType(myHandler, units, UnitType.Rocket);

            takeTurnByType(myHandler, units, UnitType.Ranger);

            takeTurnByType(myHandler, units, UnitType.Worker);

            takeTurnByType(myHandler, units, UnitType.Healer);
            
            // takeTurnByType(myHandler, units, UnitType.Mage);
            
            gc.nextTurn();
        }
    }

    private void refreshTargets(VecUnit units) {
        Unit unit;
        for (int i = 0; i < units.size(); i++) {
            unit = units.get(i);
            if (unit.team() == enemyTeam && unit.location().isOnMap()) {
                tm.addTarget(unit.location().mapLocation());                
            }
        }
    }

    private void globalValues() {
        enemyTeam = Utils.getOtherTeam(gc.team());
        map = gc.startingMap(Planet.Mars);                
    }

    public void takeTurnByType(Map<Integer,UnitHandler> myHandler, VecUnit units, UnitType unitType) {
        for(int i = 0; i < units.size(); i ++) {
            Unit unit = units.get(i);
            if(unit.unitType() == unitType && !unit.location().isInGarrison() && !unit.location().isInSpace()) {
                myHandler.get(unit.id()).takeTurn(unit);
            }
        }
    }

    public void assignHandler(Map<Integer,UnitHandler> myHandler, Unit unit) {

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
            // case Mage:
            //     newHandler = new MarsMageHandler(this, gc, unit.id(), rng);
            // case Healer:
            //     newHandler = new MarsHealerHandler(this, gc, unit.id(), rng);
            default:
                break;
        }
        
        myHandler.put(unit.id(), newHandler);
    }
    
    public Planet getPlanet() {
        return Planet.Mars;
    }
}