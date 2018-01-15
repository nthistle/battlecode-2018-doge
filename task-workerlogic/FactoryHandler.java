import bc.*;
import java.util.Random;

public class FactoryHandler extends UnitHandler {

    public FactoryHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }
    
    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        

    public void takeTurn(Unit unit) {
        VecUnitID garrison = unit.structureGarrison();
        if (garrison.size() > 0) {
            Direction d = Utils.getRandomDirection(Utils.directionList.toArray(new Direction[Utils.directionList.size()]), rng);
            try {
                if (gc.canUnload(unit.id(), d)) {
                    gc.unload(unit.id(), d);
                    System.out.println("Unloaded a Knight!");
                }
            } catch(Exception e) {e.printStackTrace();}
        }                    
        if (gc.canProduceRobot(unit.id(), UnitType.Knight)) {
            // gc.produceRobot(unit.id(), UnitType.Knight);
            // System.out.println("Produced a Knight!");
        }                            
    }
}