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
import java.util.Queue;

public class EarthController extends PlanetController
{
    public EarthController(GameController gc, Random rng) {
        super(gc, rng);
    }

    protected LaunchingLogicHandler llh;
    
    public PlanetMap map;
    public Team enemyTeam;

    public VecUnit units;
    public HashMap<UnitType, Integer> robotCount = new HashMap<UnitType, Integer>();
    public HashMap<Integer, UnitHandler> myHandler;
    public List<Queue<UnitType>> factoryBuildQueues = new ArrayList<Queue<UnitType>>();

    public boolean isSavingForFactory = false;
    public boolean isSavingForRocket = false;
    public int rocketsBuilt = 0;

    public void control() {
    
        System.out.println("Earth Controller initiated");
    
        globalValues();

        myHandler = new HashMap<Integer, UnitHandler>();

        initializeTMTargets();

        queueResearch();
        
        llh = new LaunchingLogicHandler(this, gc, -1, rng);

        while (true) {
        
            System.out.println("Round #" + gc.round() + ", (" + gc.getTimeLeftMs() + " ms left");

            System.runFinalization();
            System.gc();

            VecUnit allUnits = gc.units();
            VecUnit units = gc.myUnits();

            for(int i = 0; i < allUnits.size(); i ++) {
                // this is probably going to clog targetingmaster to high hell but who cares rn
                Unit uu = allUnits.get(i);
                if(uu.team() == enemyTeam && !uu.location().isInGarrison() && !uu.location().isInSpace() && uu.location().isOnPlanet(Planet.Earth)) {
                    tm.addTarget(uu.location().mapLocation());
                    break;
                }
            }

            
            
            refreshRobotCount(units);
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                    assignHandler(myHandler, unit);
                }
            }

            llh.takeTurn();

            takeTurnByType(myHandler, units, UnitType.Factory);

            takeTurnByType(myHandler, units, UnitType.Ranger);

            takeTurnByType(myHandler, units, UnitType.Knight);

            takeTurnByType(myHandler, units, UnitType.Worker);

            takeTurnByType(myHandler, units, UnitType.Rocket);

            gc.nextTurn();
        }
    }

    public void queueResearch() {        
        gc.queueResearch(UnitType.Ranger);
        gc.queueResearch(UnitType.Worker);
        gc.queueResearch(UnitType.Rocket);
        gc.queueResearch(UnitType.Ranger);
        gc.queueResearch(UnitType.Rocket);
        gc.queueResearch(UnitType.Rocket);
        gc.queueResearch(UnitType.Worker);
        gc.queueResearch(UnitType.Worker);
        gc.queueResearch(UnitType.Worker);
    }

    public void initializeTMTargets() {
        VecUnit startingUnits = gc.startingMap(gc.planet()).getInitial_units();
        for(int i = 0; i < startingUnits.size(); i ++) {
            if(startingUnits.get(i).team() == enemyTeam) {
                tm.addTarget(startingUnits.get(i).location().mapLocation());
            }
        }
    }
        
    public int getRobotCount(UnitType type) {
        return this.robotCount.get(type);
    }

    public void incrementRobotCount(UnitType type) {
        this.robotCount.put(type, getRobotCount(type)+1);
    }

    private void refreshRobotCount(VecUnit units) {
        for(UnitType ut : UnitType.values()) {
            robotCount.put(ut, 0);
        }
        for(int i = 0; i < units.size(); i ++) {
            UnitType ut = units.get(i).unitType();
            incrementRobotCount(ut);
        }
    }

    // initialize global values
    private void globalValues() {
        enemyTeam = Utils.getOtherTeam(gc.team());
        map = gc.startingMap(Planet.Earth);        
    }

    public void takeTurnByType(HashMap<Integer,UnitHandler> myHandler, VecUnit units, UnitType unitType) {
        for(int i = 0; i < units.size(); i ++) {
            Unit unit = units.get(i);
            if(unit.unitType() == unitType) {
                myHandler.get(unit.id()).takeTurn(unit);
            }
        }
    }

    public void assignHandler(HashMap<Integer,UnitHandler> myHandler, Unit unit) {

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
            case Rocket:
                newHandler = new RocketHandler(this, gc, unit.id(), rng, this.llh, RocketHandler.FIRST_CONTACT_CREW);
                break;
            default:
                break;
        }
        
        myHandler.put(unit.id(), newHandler);
    }
    
    public Planet getPlanet() {
        return Planet.Earth;
    }
}