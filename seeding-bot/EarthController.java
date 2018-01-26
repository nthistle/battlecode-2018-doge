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

public class EarthController extends PlanetController
{
    public EarthController(GameController gc, Random rng) {
        super(gc, rng);
    }

    protected LaunchingLogicHandler llh;
    
    public PlanetMap map;
    public Team enemyTeam;
    
    public Map<UnitType, Integer> robotCount = new EnumMap<UnitType, Integer>(UnitType.class);
    public Map<Integer, UnitHandler> myHandler;
    public List<Queue<UnitType>> factoryBuildQueues = new ArrayList<Queue<UnitType>>();

    // public Queue<Integer> attackQueue = new LinkedList<Integer>();

    public int amLoadingRocket = 0;

    public boolean noEnemies = true;
    public long maxUnits = Integer.MAX_VALUE;

    public boolean isSavingForFactory = false;
    public long factoryRequestRound = 0;
    public boolean isSavingForRocket = false;
    public long rocketRequestRound = 0;
    public long rocketsBuilt = 0;

    public void control() {
    
        System.out.println("Earth Controller initiated");
    
        globalValues();

        myHandler = new HashMap<Integer, UnitHandler>();

        initializeTMTargets();

        queueResearch();
        
        llh = new LaunchingLogicHandler(this, gc, -1, rng);

        while (true) {
        
            System.out.println("Round #" + gc.round() + ", (" + gc.getTimeLeftMs() + " ms left)");

            System.runFinalization();
            System.gc();

            VecUnit allUnits = gc.units();
            VecUnit units = gc.myUnits();

            noEnemies = true;

            for(int i = 0; i < allUnits.size(); i ++) {
                // this is probably going to clog targetingmaster to high hell but who cares rn
                Unit uu = allUnits.get(i);
                if(uu.team() == enemyTeam && !uu.location().isInGarrison() && !uu.location().isInSpace() && uu.location().isOnPlanet(Planet.Earth)) {
                    tm.addTarget(uu.location().mapLocation());
                    break;
                }
            }

            rocketStatus();
            
            refreshRobotCount(units);

            refreshTargets(allUnits);
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                    assignHandler(myHandler, unit);
                }
            }

            llh.takeTurn();

            // Workers should move early, since they'll be finishing building other units
            // and will be harvesting more Karbonite for us earlier in the turn
            takeTurnByType(myHandler, units, UnitType.Worker);

            // Update the Factory build queues
            updateFactoryBuildQueues(myHandler, units);

            takeTurnByType(myHandler, units, UnitType.Factory);

            takeTurnByType(myHandler, units, UnitType.Ranger);

            takeTurnByType(myHandler, units, UnitType.Knight);
            
            takeTurnByType(myHandler, units, UnitType.Healer);

            takeTurnByType(myHandler, units, UnitType.Rocket);

            gc.nextTurn();
        }
    }

    public void addRocketRequestedUnits(RocketHandler rh, Unit rocket) {
        // System.out.println("Rocket is requesting some units, adding to build queues...");
        VecUnit units = gc.myUnits(); // a little inefficient, TODO optimize
        Unit unit;
        ArrayList<FactoryHandler> sameRegion = new ArrayList<FactoryHandler>();
        int targetRegion = this.pm.getRegion(rocket.location().mapLocation());
        for(int i = 0; i < units.size(); i ++) {
            unit = units.get(i);
            if(unit.unitType()!=UnitType.Factory) continue;
            if(this.pm.getRegion(unit.location().mapLocation()) == targetRegion) {
                sameRegion.add((FactoryHandler)myHandler.get(unit.id()));
            }
        }
        ArrayList<UnitType> nunits = new ArrayList<UnitType>();
        for(UnitType key : rh.stillNeeded.keySet()) {
            for(int i = 0; i < rh.stillNeeded.get(key).intValue(); i ++) {
                nunits.add(key);
            }
        }
        Collections.shuffle(nunits);
        if(sameRegion.size() == 0) return;
        int curFac = 0;
        for(int i = 0; i < nunits.size(); i ++) {
            // System.out.println("Adding " + nunits.get(i) + " to build queue");
            // sameRegion.get(curFac).addToBuildQueue(nunits.get(i));
            sameRegion.get(curFac).forceAddPriorityBuildQueue(nunits.get(i));
            curFac = (curFac+1)%sameRegion.size();
        }
        amLoadingRocket ++;
    }

    private void updateFactoryBuildQueues(Map<Integer,UnitHandler> myHandler, VecUnit units) {
        if (noEnemies && getRobotCount(UnitType.Ranger) + getRobotCount(UnitType.Healer) > maxUnits) {
            return;
        }

        int workersNecessary = 3 - this.getRobotCount(UnitType.Worker);
        // never want to get below 3 workers, have the factories URGENTLY make them

        Unit unit;
        FactoryHandler fh;
        for(int i = 0; i < units.size(); i ++) {
            unit = units.get(i);
            if(unit.unitType()!=UnitType.Factory) continue;

            fh = (FactoryHandler)myHandler.get(unit.id());
            if(workersNecessary > 0 && fh.peekBuildQueue()!=UnitType.Worker) {
                fh.clearBuildQueue();
                fh.addToBuildQueue(UnitType.Worker);
                workersNecessary--;
            }
            if(fh.getBuildQueueSize() < FactoryHandler.IDEAL_BQUEUE_SIZE) {
                fh.addToBuildQueue(this.getRandomBasePhaseUnit());
            }
            // System.out.println("Factory ID#"+i+" has build queue of length " + fh.getBuildQueueSize());
        }
    }

    private UnitType getRandomBasePhaseUnit() {
        double d = rng.nextDouble();
        // pointless to make anything but rangers right now, until other code is working
        if(d < 0.2 && gc.round() > 150 && getRobotCount(UnitType.Ranger) > 10) return UnitType.Healer;
        else return UnitType.Ranger;
    }

    private void refreshTargets(VecUnit units) {
        Unit unit;
        for (int i = 0; i < units.size(); i++) {
            unit = units.get(i);
            if (unit.team() == enemyTeam && unit.location().isOnMap()) {
                tm.addTarget(unit.location().mapLocation());
                noEnemies = false;
            }
        }
    }

    private void rocketStatus() {
        if (gc.researchInfo().getLevel(UnitType.Rocket) >= 1 && ((getRobotCount(UnitType.Worker) >= 2 && (rocketsBuilt < (int)(gc.round() / 125) || (gc.getTimeLeftMs() < 1500 && getRobotCount(UnitType.Rocket) < 1))) || (getRobotCount(UnitType.Worker) >= 1 && (gc.round() > 200 && gc.units().size() - gc.myUnits().size() > gc.myUnits().size() * 2)))) {
            isSavingForRocket = true;            
            rocketRequestRound = gc.round();
        }        
        if (gc.round() > rocketRequestRound + 40) {
            isSavingForRocket = false;
        }
    }

    // For now, only does Rangers/Workers/Rockets, and only takes us to Round 625
    private void queueResearch() {        
        gc.queueResearch(UnitType.Ranger); // Ranger I (25)
        // completed by round 25

        gc.queueResearch(UnitType.Worker); // Worker I (25)
        // completed by round 50

        gc.queueResearch(UnitType.Rocket); // Rocket I (50)
        // completed by round 100

        gc.queueResearch(UnitType.Ranger); // Ranger II (100)
        // completed by round 200

        gc.queueResearch(UnitType.Rocket); // Rocket II (100)
        // completed by round 300

        gc.queueResearch(UnitType.Rocket); // Rocket III (100)
        // completed by round 400

        gc.queueResearch(UnitType.Worker); // Worker II (75)
        // completed by round 475

        gc.queueResearch(UnitType.Worker); // Worker III (75)
        // completed by round 550

        gc.queueResearch(UnitType.Worker); // Worker IV (75)
        // completed by round 625
    }

    public void initializeTMTargets() {
        VecUnit startingUnits = gc.startingMap(gc.planet()).getInitial_units();
        for(int i = 0; i < startingUnits.size(); i ++) {
            if(startingUnits.get(i).team() == enemyTeam) {
                MapLocation temp = startingUnits.get(i).location().mapLocation();
                tm.initial.add(temp.toJson());
                tm.addTarget(temp);
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
        UnitType ut;
        for(int i = 0; i < units.size(); i ++) {
            ut = units.get(i).unitType();
            incrementRobotCount(ut);
        }
    }

    // initialize global values
    private void globalValues() {
        enemyTeam = Utils.getOtherTeam(gc.team());
        map = gc.startingMap(Planet.Earth);        
        maxUnits = (long)(Math.max((int)map.getHeight(), (int)map.getWidth()) * 3.5);
    }

    public void takeTurnByType(Map<Integer,UnitHandler> myHandler, VecUnit units, UnitType unitType) {
        Unit unit;
        UnitHandler uh;
        for(int i = 0; i < units.size(); i ++) {
            unit = units.get(i);
            if(unit.unitType() == unitType && !unit.location().isInGarrison() && !unit.location().isInSpace()) {
                uh = myHandler.get(unit.id());
                if(uh != null) uh.takeTurn(unit);
            }
        }
    }

    public RocketHandler doesRocketNeed(UnitType ut, MapLocation ml) {
        VecUnit units = gc.myUnits();
        Unit unit;
        RocketHandler rh;
        // System.out.println("Considering Unit Type " + ut);
        for(int i = 0; i < units.size(); i ++) {
            unit = units.get(i);
            if(unit.unitType()!=UnitType.Rocket) continue;

            rh = (RocketHandler)myHandler.get(unit.id());
            // System.out.println("Rocket #" + i);
            if(rh==null)
                continue;
            if(!this.pm.isConnected(ml, unit.location().mapLocation()))
                continue;
            if(!rh.stillNeeded.keySet().contains(ut))
                continue;
            if(rh.stillNeeded.get(ut) > 0) return rh;
        }
        return null;
    }

    public void assignHandler(Map<Integer,UnitHandler> myHandler, Unit unit) {

        UnitHandler newHandler = null;

        if(unit.location().isInGarrison()) return;

        if(amLoadingRocket > 0) {
            // System.out.println("Loading a rocket, does it need?");
            RocketHandler neededBy = doesRocketNeed(unit.unitType(), unit.location().mapLocation());
            if(neededBy != null) {
                // System.out.println("NEW ASTRONAUT U HOE, it's a " + unit.unitType());
                newHandler = new AstronautHandler(this, gc, unit.id(), rng, gc.unit(neededBy.id).location().mapLocation());
                neededBy.stillNeeded.put(unit.unitType(), neededBy.stillNeeded.get(unit.unitType()) - 1);
                myHandler.put(unit.id(), newHandler);
                return;
            }
        }
        
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
                newHandler = new RocketHandler(this, gc, unit.id(), rng, this.llh, llh.nextManifest());
                addRocketRequestedUnits((RocketHandler)newHandler, unit);
                break;
            case Healer:
            	newHandler = new HealerHandler(this, gc, unit.id(), rng);
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