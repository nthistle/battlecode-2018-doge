import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.LinkedList;

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

    public TargetingMaster tm;

    public void control() {
    
        System.out.println("Earth Controller iniatied");

        this.tm = new TargetingMaster(this.gc);
    
        HashMap<Integer,UnitHandler> myHandler = new HashMap<Integer,UnitHandler>();

        enemyTeam = Utils.getOtherTeam(gc.team());

        earthMap = gc.startingMap(Planet.Earth);

        VecUnit startingUnits = earthMap.getInitial_units();
        for(int i = 0; i < startingUnits.size(); i ++) {
            if(startingUnits.get(i).team()==enemyTeam) {
                tm.addTarget(startingUnits.get(i).location().mapLocation());
            }
        }

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
        
            System.out.println("Round #"+gc.round());
            System.out.println("Time used: " + gc.getTimeLeftMs());

            VecUnit allUnits = gc.units();
            for(int i = 0; i < allUnits.size(); i ++) {
                // this is probably going to clog targetingmaster to high hell but who cares rn
                Unit uu = allUnits.get(i);
                if(uu.team() == enemyTeam && !uu.location().isInGarrison() && !uu.location().isInSpace() && uu.location().isOnPlanet(Planet.Earth)) {
                    tm.addTarget(uu.location().mapLocation());
                }
            }

            //SWARM STUFF
            //TODO figure out a better starting swarmLeader position than 5,5
            //TODO figure out a better way to get new targets
            //TODO figure out a better way to queue up swarms
            VecUnit units = gc.myUnits();


            /*
            if(gc.round() >= 1 && gc.round() % 2 == 0) {
                VecUnit original = gc.startingMap(Planet.Earth).getInitial_units();
                MapLocation target = null;
                List<Unit> enemyStartingPositions = new ArrayList<>();
                for(int i = 0; i < original.size(); i++) {
                    if(original.get(i).team() != gc.team()) {
                        enemyStartingPositions.add(original.get(i));
                    }
                }
                target = enemyStartingPositions.get(this.rng.nextInt(enemyStartingPositions.size())).location().mapLocation();
                this.createSwarm(new RangerSwarm(gc), 8, new MapLocation(Planet.Earth, 0, 0), target);
            }
            */

            if(gc.round() >= 1 && gc.round() % 15 == 0) {
            	System.out.println("Created a new swarm");
            	MapLocation a = findTarget(null);
                this.createSwarm(new RangerSwarm(gc), 8, a);
            }

            //END SWARM STUFF


            ResearchInfo research = gc.researchInfo();
            if (research.getLevel(UnitType.Ranger) != 3 && !research.hasNextInQueue()) {
                gc.queueResearch(UnitType.Ranger);
            }
            if (research.getLevel(UnitType.Knight) == 3 && research.getLevel(UnitType.Worker) != 4 && !research.hasNextInQueue()) {
                gc.queueResearch(UnitType.Worker);
            }

            robotCount = new HashMap<UnitType, Integer>(); 
            
            units = gc.myUnits();
            
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

            Iterator<Swarm> it = this.getSwarm().iterator();
            while(it.hasNext()) {
                Swarm id = it.next();
                if(id.getUnits().size() == 0)
                    it.remove();
            }

            //SWARM STUFF
            for (int i = 0; i < units.size(); i++) {    
                Unit unit = units.get(i);     
                boolean isPartOfSwarm = false;
                for(int j = 0; j < this.getSwarm().size(); j++) {
                    if(this.getSwarm().get(j).getUnits().contains(unit.id())) {
                        isPartOfSwarm = true;
                        break;
                    }
                }
                
                if(!isPartOfSwarm)
                    myHandler.get(unit.id()).takeTurn(unit);
            }
            for(int i = 0; i < this.getSwarm().size(); i++) {
                if(this.getSwarm().get(i).getUnits().size() > 0) {
                	try {
	                    this.getSwarm().get(i).takeTurn();
	                } catch (Exception e) {}
                }
            }
            //END SWARM STUFF
            

            gc.nextTurn();
        }
    }
    
    public MapLocation findTarget(MapLocation myLocation) {
        if (myLocation == null) {
            VecUnit original = gc.startingMap(Planet.Earth).getInitial_units();            
            List<Unit> startingPositions = new ArrayList<>();
            for(int i = 0; i < original.size(); i++) {
                if(original.get(i).team() == gc.team()) {
                    startingPositions.add(original.get(i));
                }
            }
            myLocation = startingPositions.get(this.rng.nextInt(startingPositions.size())).location().mapLocation();            
        }
        MapLocation myTarget = null;
        PathField pathToTarget = null;
        for(int i = 0; i < tm.getNumTargets(); i ++) {
            myTarget = tm.getTarget(i);
            pathToTarget = pm.getPathField(myTarget);
            if(pathToTarget.isPointSet(myLocation))
                break;
        }
        return myTarget;        
    }

    public Planet getPlanet() {
        return Planet.Earth;
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

    /*
    @Override
    public void requestSwarm(int goalSize, MapLocation target, UnitType c) {
        for(int i = 0; i < this.getSwarm().size(); i++) {
            Swarm d = this.getSwarm().get(i);
            if(d.swarmTarget == null && d.getUnits().size() >= goalSize) {
                if(d instanceof RangerSwarm && c == UnitType.Ranger) {
                    d.setSwarmTarget(target);
                    return;
                }
            }
        }
        VecUnit units = gc.myUnits();
        List<Integer> typeLocations = new ArrayList<Integer>();
        for(int i = 0; i < units.size(); i++) {
            Unit want = units.get(i);
            if(want.unitType() == c) {
                if(want.health() >= 90L && !want.location().isInGarrison())
                    typeLocations.add(want.id());
            }
        }
        if(typeLocations.size() < goalSize) {
            this.createSwarm(new RangerSwarm(gc), goalSize, new MapLocation(Planet.Earth, 0, 0), target);
            return;
        } else {
            RangerSwarm swarm = new RangerSwarm(gc);
            TreeMap<Long, Integer> metric = new TreeMap<Long, Integer>();
            for(Integer a : typeLocations) {
                metric.put(Utils.distanceSquaredTo(gc.unit(a).location().mapLocation(), target), a);
            }
            int counter = 0;
            for(Long a : metric.keySet()) {
                swarm.addUnit(metric.get(a));
                counter += 1;
                if(counter == goalSize)
                    break;
            }
            List<Integer> swarmUnits = swarm.getUnits();
            int sumX = 0;
            int sumY = 0;
            for(Integer a : swarmUnits) {
                sumX += gc.unit(a).location().mapLocation().getX();
                sumY += gc.unit(a).location().mapLocation().getY();
            }
            MapLocation centroid = new MapLocation(Planet.Earth, ((int) sumX / swarmUnits.size()), ((int) sumY / swarmUnits.size()));
            swarm.setSwarmLeader(centroid);
            swarm.setSwarmTarget(target);
            swarm.setPath(this.pm.generatePathField(target));
            this.getSwarm().add(swarm);
        }
    }
    */
}