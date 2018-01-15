import bc.*;
import java.util.Random;
import java.util.HashMap;

public class EarthController extends PlanetController
{
    public EarthController(GameController gc, Random rng) {
        super(gc, rng);
    }
    
    public PlanetMap earthMap;

    public Team enemyTeam;
    public MapLocation targetLocation;
    public MapLocation threatLocation;

    public HashMap<UnitType, Integer> robotCount;

    public void control() {
    
        System.out.println("Earth Controller initiated");
            
        HashMap<Integer,UnitHandler> myHandler = new HashMap<Integer,UnitHandler>();

        enemyTeam = Utils.getOtherTeam(gc.team());

        earthMap = gc.startingMap(Planet.Earth);
        MapLocation startLocation = gc.myUnits().get(0).location().mapLocation();
        targetLocation = new MapLocation(Planet.Earth, (int)earthMap.getWidth() - startLocation.getX(), (int)earthMap.getHeight() - startLocation.getY());     

        while (true) {        

            System.out.println("Round #"+gc.round());

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

            for (int i = 0; i < units.size(); i++) {                
                Unit unit = units.get(i);
                myHandler.get(unit.id()).takeTurn(unit);
            }

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