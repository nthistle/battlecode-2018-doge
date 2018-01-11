import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class Player {
    
    public static void main(String[] args) {
    
        Random rand = new Random();
        
        GameController gc = new GameController();
        Team myTeam = gc.team();
        
        Map<Integer,UnitHandler> myHandler = new HashMap<Integer,UnitHandler>();

        while (true) {
        
            System.out.println("Round #"+gc.round());
            
            VecUnit units = gc.myUnits();
            
            for(int i = 0; i < units.size(); i ++) {
                registerUnit(gc, rand, myHandler, units.get(i));
            }
            
            // all factories should move first, so we unload then move
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                if(unit.unitType()==UnitType.Factory)
                    myHandler.get(unit.id()).takeTurn(unit);
            }
            
            // now everything else can move
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                if(unit.unitType()!=UnitType.Factory)
                    myHandler.get(unit.id()).takeTurn(unit);
            }
            
            gc.nextTurn();
        }
    }
    
    // registers the unit into the hashmap with a new handler, if it isn't
    // already in there.
    public static void registerUnit(GameController gc, Random rand, Map<Integer,UnitHandler> handlermap, Unit unit) {
   
        if(!handlermap.containsKey(unit.id())) {
        
            UnitHandler newHandler = null;
            
            switch(unit.unitType()) {
                case Factory:
                  newHandler = new FactoryHandler(gc, unit.id(), rand);
                  break;
                case Knight:
                  newHandler = new KnightHandler(gc, unit.id(), rand);
                  break;
                case Ranger:
                  newHandler = new RangerHandler(gc, unit.id(), rand);
                  break;
                case Worker:
                  newHandler = new WorkerHandler(gc, unit.id(), rand);
                  break;
                default:
                  break;
            }
            
            handlermap.put(unit.id(), newHandler);
        }
    }
}