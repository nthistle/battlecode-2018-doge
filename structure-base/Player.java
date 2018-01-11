import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class Player {
    
    public static void main(String[] args) {
    
        Random rand = new Random();
        
        GameController gc = new GameController();
        Team myTeam = gc.team();
        
        HashMap<Integer,UnitHandler> myHandlers = new HashMap<Integer,UnitHandler>();

        while (true) {
        
            System.out.println("Round #"+gc.round());
            
            VecUnit units = gc.myUnits();
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                
                    UnitHandler newHandler = null;
                    
                    switch(unit.type()) {
                        case UnitType.Factory:
                          newHandler = new FactoryHandler(gc, rand);
                          break;
                        case UnitType.Knight:
                          newHandler = new KnightHandler(gc, rand);
                          break;
                        case UnitType.Ranger:
                          newHandler = new RangerHandler(gc, rand);
                          break;
                        case UnitType.Worker:
                          newHandler = new WorkerHandler(gc, rand);
                          break;
                        default:
                          break;
                    }
                    
                    myHandler.put(unit.id(), newHandler);
                }
                myHandler.get(unit.id()).takeTurn();
            }
            gc.nextTurn();
        }
    }
}