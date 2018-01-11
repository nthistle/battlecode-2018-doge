import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class Player {
    
    public static void main(String[] args) {
    
        Random rand = new Random();
        
        GameController gc = new GameController();
        Team myTeam = gc.team();
        
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
                    
                    myHandler.put(unit.id(), newHandler);
                }
                myHandler.get(unit.id()).takeTurn(unit);
            }
            gc.nextTurn();
        }
    }
}