import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Random;

public class EarthController extends PlanetController
{
    public EarthController(GameController gc, Random rng) {
        super(gc, rng);
    }
    
    public PlanetMap map;
    public Team enemyTeam;

    public VecUnit units;
    public HashMap<UnitType, Integer> robotCount = new HashMap<UnitType, Integer>();
    public HashMap<Integer,UnitHandler> myHandler;
    
    public void control() {
    
        System.out.println("Earth Controller initiated");
    
        globalValues();

        myHandler = new HashMap<Integer,UnitHandler>();

        while (true) {
        
            System.out.println("Round #" + gc.round() + ", (" + gc.getTimeLeftMs() + " ms left");
            System.gc();
            
            VecUnit units = gc.myUnits();

            refreshRobotCount(units);
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                    assignHandler(myHandler, unit);
                }
            }

            takeTurnByType(myHandler, units, UnitType.Factory);

            takeTurnByType(myHandler, units, UnitType.Ranger);

            takeTurnByType(myHandler, units, UnitType.Knight);

            takeTurnByType(myHandler, units, UnitType.Worker);

            gc.nextTurn();
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
            default:
                break;
        }
        
        myHandler.put(unit.id(), newHandler);
    }
    
    public Planet getPlanet() {
        return Planet.Earth;
    }
}