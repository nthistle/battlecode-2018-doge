import bc.*;
import java.util.Random;

public class FactoryHandler extends UnitHandler {

    public FactoryHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
        if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
            gc.produceRobot(this.id, UnitType.Ranger);
        }
        VecUnitID garrison = unit.structureGarrison();
        if(garrison.size() > 0) {
            for(int i = 0; i < 8; i ++) {
                Direction unloadDir = Utils.getRandomDirection(Direction.values(), this.rng);
                if(gc.canUnload(this.id, unloadDir)) {
                    gc.unload(this.id, unloadDir);
                }
            }
        }
    }
}