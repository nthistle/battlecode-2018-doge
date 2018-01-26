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
    
    public void control() {
    
        System.out.println("Mars Controller initiated");

        globalValues();
    
        myHandler = new HashMap<Integer, UnitHandler>();        

        while (true) {
        
            System.out.println("Round #" + gc.round() + ", (" + gc.getTimeLeftMs() + " ms left)");

            System.runFinalization();
            System.gc();            
            
            VecUnit allUnits = gc.units();
            VecUnit units = gc.myUnits();            

            for(int i = 0; i < allUnits.size(); i ++) {
                // this is probably going to clog targetingmaster to high hell but who cares rn
                Unit uu = allUnits.get(i);
                if(uu.team() == enemyTeam && !uu.location().isInGarrison() && !uu.location().isInSpace() && uu.location().isOnPlanet(Planet.Mars)) {
                    tm.addTarget(uu.location().mapLocation());
                    break;
                }
            }
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                    assignHandler(myHandler, unit);
                }
            }

            takeTurnByType(myHandler, units, UnitType.Rocket);

            takeTurnByType(myHandler, units, UnitType.Ranger);

            takeTurnByType(myHandler, units, UnitType.Worker);

            gc.nextTurn();
        }
    }

    private void globalValues() {
        enemyTeam = Utils.getOtherTeam(gc.team());
        map = gc.startingMap(Planet.Mars);                
    }

    public void takeTurnByType(HashMap<Integer,UnitHandler> myHandler, VecUnit units, UnitType unitType) {
        for(int i = 0; i < units.size(); i ++) {
            Unit unit = units.get(i);
            if(unit.unitType() == unitType && !unit.location().isInGarrison() && !unit.location().isInSpace()) {
                myHandler.get(unit.id()).takeTurn(unit);
            }
        }
    }

    public void assignHandler(HashMap<Integer,UnitHandler> myHandler, Unit unit) {

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
            default:
                break;
        }
        
        myHandler.put(unit.id(), newHandler);
    }
    
    public Planet getPlanet() {
        return Planet.Mars;
    }
}