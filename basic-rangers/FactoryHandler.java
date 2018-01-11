import bc.*;
import java.util.Random;

public class FactoryHandler {

    public FactoryHandler(GameController gc, int id, Random rng) {
        super(gc, id, rng);
    }
    
    public void takeTurn() {
        VecUnitID garrison = unit.structureGarrison();
        if(garrison.size() > 0) {
            for(int i = 0; i < 5; i ++) {
                Direction unloadDir = Utils.getRandomDirection(Direction.values(), this.rng);
                if(gc.canUnload(this.id, unloadDir)) {
                    gc.unload(this.id, unloadDir);
                }
            }
        }
        if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
            gc.produceRobot(this.id, UnitType.Ranger);
        }
    }
}