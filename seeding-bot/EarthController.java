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

    public static final int AUTO_ROCKET_FLIP = 30;
    public static final int AUTO_FACTORY_FLIP = 30;

    protected LaunchingLogicHandler llh;
    protected MiningMaster mm;
    
    public PlanetMap map;
    public Team enemyTeam;
    
    public Map<UnitType, Integer> robotCount = new EnumMap<UnitType, Integer>(UnitType.class);
    public List<Queue<UnitType>> factoryBuildQueues = new ArrayList<Queue<UnitType>>();
    
    public Direction[][] rocketWarning; 

    // public Queue<Integer> attackQueue = new LinkedList<Integer>();

    public int amLoadingRocket = 0;

    public boolean noEnemies = true;    

    public boolean isSavingForFactory = false;
    public long factoryRequestRound = 0;
    public boolean isSavingForRocket = false;
    public long rocketRequestRound = 0;
    public long rocketsBuilt = 0;

    public int eworkerCount = 0;
    public int queuedWorkers = 0;

    public void control() {
    
        System.out.println("Earth Controller initiated");
    
        globalValues();

        myHandler = new HashMap<Integer, UnitHandler>();

        initializeTMTargets();

        queueResearch();
        
        llh = new LaunchingLogicHandler(this, gc, -1, rng);
        mm = new MiningMaster(this);
        mm.generate();
        
        rocketWarning = new Direction[(int)this.map.getWidth()][(int)this.map.getHeight()];
        
        for(int i = 0; i < rocketWarning.length; i++) {
        	for(int j = 0; j < rocketWarning[i].length; j++) {
        		rocketWarning[i][j] = Direction.Center;
        	}
        }

        initialAssign();

        while (true) {
                    
            System.out.println("Round #" + gc.round() + ", (" + gc.getTimeLeftMs() + " ms left)");

            System.runFinalization();
            System.gc();

            VecUnit allUnits = gc.units();
            VecUnit units = gc.myUnits();

            HashSet<Integer> aliveIDs = new HashSet<Integer>();
            for(int i = 0; i < units.size(); i ++) {
                aliveIDs.add(units.get(i).id());
            }

            for(int id : new HashSet<Integer>(myHandler.keySet())) {
                if(!aliveIDs.contains(id)) { // unit has died
                    System.out.println(id + " has died!");
                    myHandler.get(id).handleDeath();
                    myHandler.remove(id);
                }
            }

            noEnemies = true;

            // for(int i = 0; i < allUnits.size(); i ++) {
            //     // this is probably going to clog targetingmaster to high hell but who cares rn
            //     Unit uu = allUnits.get(i);
            //     if(uu.team() == enemyTeam && !uu.location().isInGarrison() && !uu.location().isInSpace() && uu.location().isOnPlanet(Planet.Earth)) {
            //         tm.addTarget(uu.location().mapLocation());
            //         break;
            //     }
            // }

            rocketStatus();
            factoryStatus();
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                    assignHandler(myHandler, unit);
                }
            }

            refreshRobotCount(units);

            refreshTargets(allUnits);

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

    public void initialAssign() {
        VecUnit units = gc.myUnits();                
        for (int i = 0; i < units.size(); i++) {            
            Unit unit = units.get(i);
            if (myHandler.containsKey(unit.id())) {
                continue;
            }
            MapLocation tempLocation = unit.location().mapLocation();            
            int startX = tempLocation.getX() - 7;
            int startY = tempLocation.getY() - 7;
            int endX = startX + 14;
            int endY = startY + 14;
            int[][] tempMoney = mm.initialKarboniteLocationsOriginal;
            int totalMoney = 0;            
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {                    
                    if (x >= 0 && x < tempMoney.length && y >= 0 && y < tempMoney[0].length && tempLocation.isWithinRange(unit.visionRange(), new MapLocation(Planet.Earth, x, y))) {
                        totalMoney += tempMoney[x][y];                        
                    }                    
                }
            }
            if (totalMoney >= 50) {
                myHandler.put(unit.id(), new WorkerHandler(this, gc, unit.id(), rng));
            } else {
                if (units.size() == 1) {
                    myHandler.put(unit.id(), new WorkerHandler(this, gc, unit.id(), rng));
                    ((WorkerHandler)myHandler.get(unit.id())).solo = true;                    
                } else {
                    VecUnit nearby = gc.senseNearbyUnitsByTeam(tempLocation, unit.visionRange(), gc.team());
                    if (nearby.size() == 1) {
                        //myHandler.put(unit.id(), new MiningWorkerHandler(this, gc, unit.id(), rng, mm));
                        if(unit.unitType() == UnitType.Worker) {
                            System.out.println("Just requested a brand-new miner");
                            mm.convertToMiner(unit.id());
                        }
                    } else {
                        myHandler.put(unit.id(), new WorkerHandler(this, gc, unit.id(), rng));
                        for (int j = 0; j < nearby.size(); j++) {
                            Unit nearbyUnit = nearby.get(j);
                            if (unit.id() == nearbyUnit.id()) {
                                continue;
                            }
                            if (pm.isConnected(tempLocation, nearbyUnit.location().mapLocation())) {
                                //myHandler.put(nearbyUnit.id(), new MiningWorkerHandler(this, gc, unit.id(), rng, mm));
                                if(nearbyUnit.unitType() == UnitType.Worker) {
                                    System.out.println("Just requested a brand-new miner");
                                    mm.convertToMiner(nearbyUnit.id());
                                }
                            }
                        }                        
                    }
                }
                            
            }            
        }
    }

    public static int ROCKET_LOADING_THRESH_DIST = 12;

    public void addRocketRequestedUnits(RocketHandler rh, Unit rocket) {
        System.out.println("Rocket is adding requested units!...");
        // System.out.println("Rocket is requesting some units, adding to build queues...");
        VecUnit units = gc.myUnits(); // a little inefficient, TODO optimize
        Unit unit;
        ArrayList<FactoryHandler> sameRegion = new ArrayList<FactoryHandler>();
        int targetRegion = this.pm.getRegion(rocket.location().mapLocation());
        for(int i = 0; i < units.size(); i ++) {
            unit = units.get(i);
            if(unit.unitType()!=UnitType.Factory) continue;
            if(this.pm.getRegion(unit.location().mapLocation()) == targetRegion) {
                // we're in the same region
                // but are we a reasonable distance?
                if(this.pm.isCached(rocket.location().mapLocation()) &&
                    this.pm.getCachedPathField(rocket.location().mapLocation()).getDistanceAtPoint(unit.location().mapLocation()) < ROCKET_LOADING_THRESH_DIST) {
                    sameRegion.add((FactoryHandler)myHandler.get(unit.id()));
                }
            }
        }
        ArrayList<UnitType> nunits = new ArrayList<UnitType>();
        for(UnitType key : rh.stillNeeded.keySet()) {
            for(int i = 0; i < rh.stillNeeded.get(key).intValue(); i ++) {
                nunits.add(key);
            }
        }
        System.out.println("  Same region size: " + sameRegion.size());
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
        if (noEnemies && tm.targets.size() <= 1 && getRobotCount(UnitType.Ranger) + getRobotCount(UnitType.Healer) > 100) {
            return;
        }

        int workersNecessary = 3 - this.getEWorkerCount() - queuedWorkers;
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
                queuedWorkers++;
                workersNecessary--;                
            }
            if(fh.getBuildQueueSize() < FactoryHandler.IDEAL_BQUEUE_SIZE) {
                fh.addToBuildQueue(this.getRandomBasePhaseUnit());
            }
            // System.out.println("Factory ID#"+i+" has build queue of length " + fh.getBuildQueueSize());
        }
    }

    private UnitType getRandomBasePhaseUnit() {
        double d;
        // d = rng.nextDouble();
        // if(d < 0.35 && gc.round() > 150 && getRobotCount(UnitType.Ranger) > 10) {
        //     return UnitType.Healer;
        // } else {
        d = rng.nextDouble();
        // if(gc.round() < 150 && d < 0.1 && getRobotCount(UnitType.Ranger) > 5 && getRobotCount(UnitType.Worker) - eworkerCount < 6) {
        //     return UnitType.Worker;
        // }
        if (getRobotCount(UnitType.Ranger) > 3 && getRobotCount(UnitType.Healer) < 2) {
            return UnitType.Healer;
        }
        if (getRobotCount(UnitType.Healer) < (int)(0.5 * getRobotCount(UnitType.Ranger))) {
            return UnitType.Ranger;
        }
        if(d < 0.4 && getRobotCount(UnitType.Ranger) > 6) return UnitType.Healer;
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
        if (gc.researchInfo().getLevel(UnitType.Rocket) >= 1 && getRobotCount(UnitType.Rocket) <= 2 && ((getRobotCount(UnitType.Worker) >= 2 && ((gc.round() > 250 && rocketsBuilt < (int)(gc.round() / 125) - 1)
            || (gc.getTimeLeftMs() < 1500 && getRobotCount(UnitType.Rocket) < 1)))
            || (getRobotCount(UnitType.Worker) >= 1 && (gc.round() > 200 && gc.units().size() - gc.myUnits().size() > gc.myUnits().size() * 2)))) {
            isSavingForRocket = true;
            rocketRequestRound = gc.round();
        }
        if (gc.round() > rocketRequestRound + AUTO_ROCKET_FLIP) {
            isSavingForRocket = false;
        }
    }

    private void factoryStatus() {
        // logic here to scale up factories
        // BIG TODO
        if (gc.round() > factoryRequestRound + AUTO_FACTORY_FLIP) {
            isSavingForFactory = false;
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

    public void decrementEWorkerCount() {
        this.eworkerCount--;
    }

    public void incrementEWorkerCount() {
        this.eworkerCount ++;
    }

    public int getEWorkerCount() {
        return this.eworkerCount;
    }
        
    public int getRobotCount(UnitType type) {
        return this.robotCount.get(type);
    }

    public void incrementRobotCount(UnitType type) {
        this.robotCount.put(type, getRobotCount(type)+1);
    }

    private void refreshRobotCount(VecUnit units) {
        this.eworkerCount = 0;
        for(UnitType ut : UnitType.values()) {
            robotCount.put(ut, 0);
        }
        UnitType ut;
        for(int i = 0; i < units.size(); i ++) {
            ut = units.get(i).unitType();
            if(ut == UnitType.Worker && myHandler.get(units.get(i).id()) != null && myHandler.get(units.get(i).id()) instanceof WorkerHandler)
                this.eworkerCount ++;
            incrementRobotCount(ut);
        }
    }

    // initialize global values
    private void globalValues() {
        enemyTeam = Utils.getOtherTeam(gc.team());
        map = gc.startingMap(Planet.Earth);                
    }

    public void takeTurnByType(Map<Integer,UnitHandler> myHandler, VecUnit units, UnitType unitType) {
        Unit unit;
        UnitHandler uh;
        for(int i = 0; i < units.size(); i ++) {
            unit = units.get(i);
            // if (unitType == UnitType.Worker) {
            //     System.out.println(unitType + " " + unit.id());
            // }
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
            if(!this.pm.isCached(unit.location().mapLocation()))
                continue;
            if(this.pm.getCachedPathField(unit.location().mapLocation()).getDistanceAtPoint(ml) >= ROCKET_LOADING_THRESH_DIST)
                continue;
            if(!rh.stillNeeded.keySet().contains(ut))
                continue;
            if(rh.stillNeeded.get(ut) > 0) return rh;
        }
        return null;
    }

    public void assignHandler(Map<Integer,UnitHandler> myHandler, Unit unit) {

        if(myHandler.containsKey(unit.id())) return; // if someone else already assigned this id, don't reassign it
        if(unit.location().isInGarrison()) return; // we like to wait until our factory/rockets unload these things to assign

        UnitHandler newHandler = null;

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
                if(this.getEWorkerCount() < 3 || mm.totalValue() < 200) {                    
                    newHandler = new WorkerHandler(this, gc, unit.id(), rng);
                } else {
                    //newHandler = new MiningWorkerHandler(this, gc, unit.id(), rng, this.mm);
                    if(unit.unitType() == UnitType.Worker) {
                        System.out.println("Just requested a brand-new miner");
                        mm.convertToMiner(unit.id());
                        return;
                    }
                }
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