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

            if(gc.round() == 100)
                this.createSwarm(new RangerSwarm(gc), 8, new MapLocation(Planet.Earth, 5, 5), new MapLocation(Planet.Earth, 5, 5));

            if(gc.round() == 300)
                this.getSwarm().get(0).setPath(pm.generatePathField(new MapLocation(Planet.Earth, 5, 20)));


            if(gc.round() == 600)
                this.getSwarm().get(0).setPath(pm.generatePathField(new MapLocation(Planet.Earth, 30, 30)));


            if(gc.round() == 900)
                this.getSwarm().get(0).setPath(pm.generatePathField(new MapLocation(Planet.Earth, 10, 10)));

            /*
            if(gc.round() == 101)
                this.createSwarm(new RangerSwarm(gc), 12, new MapLocation(Planet.Earth, 5, 20));
            */

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
                boolean isPartOfSwarm = false;
                for(int j = 0; j < this.getSwarm().size(); j++) {
                    if(this.getSwarm().get(j).getUnits().contains(unit.id())) {
                        isPartOfSwarm = true;
                        break;
                    }
                }
                //temporary because i want to put all rangers into a swarm

                if(!isPartOfSwarm)
                    myHandler.get(unit.id()).takeTurn(unit);
            }
            for(int i = 0; i < this.getSwarm().size(); i++) {
                if(this.getSwarm().get(i).getUnits().size() > 0)
                    this.getSwarm().get(i).takeTurn();
            }
            gc.nextTurn();
        }
    }
    
    public Planet getPlanet() {
        return Planet.Earth;
    }
}