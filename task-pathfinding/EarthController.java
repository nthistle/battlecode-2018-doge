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
    
        System.out.println("Earth Controller iniatied");
    
        HashMap<Integer,UnitHandler> myHandler = new HashMap<Integer,UnitHandler>();

        while (true) {
        
            System.out.println("Round #"+gc.round());
            
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
                myHandler.get(unit.id()).takeTurn(unit);
            }
            gc.nextTurn();
        }
    }
    
    public Planet getPlanet() {
        return Planet.Earth;
    }

    public MapLocation getTarget() {

        PlanetMap startingMap = gc.startingMap(gc.planet());
        VecUnit startingUnits = startingMap.getInitial_units();

        Team enemy = Utils.getOtherTeam(gc.team());
        
        for(int i = 0; i < startingUnits.size(); i ++) {
            if(startingUnits.get(i).team()==enemy) {
                return startingUnits.get(i).location().mapLocation();
            }
        }
        return null;
    }
}