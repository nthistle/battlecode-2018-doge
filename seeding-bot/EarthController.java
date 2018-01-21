import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Random;

public class EarthController extends PlanetController
{
    public EarthController(GameController gc, Random rng) {
        super(gc, rng);
    }
    
    public void control() {
    
        System.out.println("Earth Controller initiated");
    
        HashMap<Integer,UnitHandler> myHandler = new HashMap<Integer,UnitHandler>();

        while (true) {
        
            System.out.println("Round #"+gc.round());
            
            VecUnit units = gc.myUnits();
            
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