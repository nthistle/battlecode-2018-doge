import bc.*;
import java.util.Random;
import java.util.HashMap;

public class EarthController extends PlanetController
{
    public EarthController(GameController gc, PathMaster pm, Random rng) {
        super(gc, pm, rng);
    }

    public PlanetMap earthMap;

    public Team enemyTeam;

    public HashMap<UnitType, Integer> robotCount;    
    public HashMap<String, Long> moneyCount;

    public void control() {
    
        System.out.println("Earth Controller iniatied");
    
        HashMap<Integer,UnitHandler> myHandler = new HashMap<Integer,UnitHandler>();

        enemyTeam = Utils.getOtherTeam(gc.team());

        earthMap = gc.startingMap(Planet.Earth);

        moneyCount = new HashMap<String, Long>();
        for (int i = 0; i < earthMap.getHeight(); i++) {
            for (int j = 0; j < earthMap.getWidth(); j++) {
                MapLocation tempLocation = new MapLocation(Planet.Earth, j, i);
                moneyCount.put(tempLocation.toJson(), gc.karboniteAt(tempLocation));
            }
        }

        while (true) {
        
            System.out.println("Round #"+gc.round());
            System.out.println("Time used: " + gc.getTimeLeftMs());

            //SWARM STUFF
            //TODO figure out a better starting swarmLeader position than 5,5
            //TODO figure out a better way to get new targets
            //TODO figure out a better way to queue up swarms
            VecUnit units = gc.myUnits();

            if(gc.round() == 1) {
                VecUnit original = gc.startingMap(Planet.Earth).getInitial_units();
                MapLocation target = null;
                List<Unit> enemyStartingPositions = new ArrayList<>();
                for(int i = 0; i < original.size(); i++) {
                    if(original.get(i).team() != gc.team()) {
                        enemyStartingPositions.add(original.get(i));
                    }
                }
                target = enemyStartingPositions.get(this.rng.nextInt(enemyStartingPositions.size())).location().mapLocation();
                this.createSwarm(new RangerSwarm(gc), 12, new MapLocation(Planet.Earth, 5, 5), target);
                this.createSwarm(new RangerSwarm(gc), 12, new MapLocation(Planet.Earth, 5, 5), target);
                this.createSwarm(new RangerSwarm(gc), 12, new MapLocation(Planet.Earth, 5, 5), target);
                this.createSwarm(new RangerSwarm(gc), 12, new MapLocation(Planet.Earth, 5, 5), target);
                this.createSwarm(new RangerSwarm(gc), 12, new MapLocation(Planet.Earth, 5, 5), target);
                this.createSwarm(new RangerSwarm(gc), 8, new MapLocation(Planet.Earth, 5, 5), target);
                this.createSwarm(new RangerSwarm(gc), 8, new MapLocation(Planet.Earth, 5, 5), target);
                this.createSwarm(new RangerSwarm(gc), 8, new MapLocation(Planet.Earth, 5, 5), target);
                this.createSwarm(new RangerSwarm(gc), 8, new MapLocation(Planet.Earth, 5, 5), target);
                this.createSwarm(new RangerSwarm(gc), 8, new MapLocation(Planet.Earth, 5, 5), target);
            }
            //END SWARM STUFF


            ResearchInfo research = gc.researchInfo();
            if (research.getLevel(UnitType.Knight) != 3 && !research.hasNextInQueue()) {
                gc.queueResearch(UnitType.Knight);
            }
            if (research.getLevel(UnitType.Knight) == 3 && research.getLevel(UnitType.Worker) != 4 && !research.hasNextInQueue()) {
                gc.queueResearch(UnitType.Worker);
            }

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

            //SWARM STUFF
            for (int i = 0; i < units.size(); i++) {                
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
                if(this.getSwarm().get(i).getUnits().size() > 0)
                    this.getSwarm().get(i).takeTurn();
            }
            //END SWARM STUFF
            

            gc.nextTurn();
        }
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
}