import bc.*;
import java.util.*;

public class EarthController extends PlanetController
{
    public EarthController(GameController gc, PathMaster pm, Random rng) {
        super(gc, pm, rng);
    }

    public PlanetMap earthMap;

    public Team enemyTeam;

    public HashMap<String, Long> moneyCount;
    public LinkedList<MapLocation> moneyLocations;

    public TargetingMaster tm;

    public LaunchingLogicHandler launchLogic; 

    public void control() {
    
        System.out.println("Earth Controller iniatied");

        this.tm = new TargetingMaster(this.gc);
    
        HashMap<Integer, UnitHandler> myHandler = new HashMap<Integer, UnitHandler>();

        enemyTeam = Utils.getOtherTeam(gc.team());

        earthMap = gc.startingMap(Planet.Earth);

        launchLogic = new LaunchingLogicHandler(this, gc, -1, rng);

        VecUnit startingUnits = earthMap.getInitial_units();
        for(int i = 0; i < startingUnits.size(); i ++) {
            if(startingUnits.get(i).team()==enemyTeam) {
                tm.addTarget(startingUnits.get(i).location().mapLocation());
            }
            else {
            	workers.add(startingUnits.get(i).id());
            	handlerManager.put(startingUnits.get(i).id(), new WorkerHandler(this, gc, startingUnits.get(i).id(), rng));
            	livingUnits.add(startingUnits.get(i).id());
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

            launchLogic.takeTurn();
            
            System.out.println(this.buildQueue);

            VecUnit enemies = gc.senseNearbyUnitsByTeam(new MapLocation(Planet.Earth, 0, 0), Long.MAX_VALUE, this.enemyTeam);
            for(int i = 0; i < enemies.size(); i ++) {
            	tm.addTarget(enemies.get(i).location().mapLocation());
                // this is probably going to clog targetingmaster to high hell but who cares rn
            }

            
            
            //SWARM STUFF
            //TODO figure out a better starting swarmLeader position than 5,5
            //TODO figure out a better way to get new targets
            //TODO figure out a better way to queue up swarms
            


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

            ResearchInfo research = gc.researchInfo();
            if(!research.hasNextInQueue()) {
            	if(research.getLevel(UnitType.Ranger) < 1) {
            		gc.queueResearch(UnitType.Ranger);
            	}
            	else if(research.getLevel(UnitType.Rocket) < 1) {
            		gc.queueResearch(UnitType.Rocket);
            	}
            	else if(research.getLevel(UnitType.Worker) < 2) {
            		gc.queueResearch(UnitType.Worker);
            	}
            	else if(research.getLevel(UnitType.Rocket) < 2) {
            		gc.queueResearch(UnitType.Rocket); 
            	}
            	else if(research.getLevel(UnitType.Worker) < 4) {
            		gc.queueResearch(UnitType.Worker);
            	}
            	//no other things ... yet
            }
            
            VecUnit units = gc.myUnits();
            
            for(int id : new HashSet<Integer>(livingUnits)) {
            	try {
            		gc.unit(id); //attempt access to unit
            	} catch(Exception e) {
            		this.livingUnits.remove(id);
            		this.handlerManager.remove(id);
            		continue;
            	}
            	if(gc.unit(id).location().isInSpace() || gc.unit(id).location().isInGarrison()) { //in garrison, in space
            		this.livingUnits.remove(id);
            		this.handlerManager.remove(id);
            		continue;
            	}
            	System.out.println(gc.unit(id).unitType().toString() + " " + id + " taking turn");
            	handlerManager.get(id).takeTurn();
            }
            
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

    
}