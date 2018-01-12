import bc.*;
import java.util.Random;

public class FactoryHandler extends UnitHandler {

    private boolean unloadFailed = false;
    
    public FactoryHandler(GameController gc, int id, Random rng) {
        super(gc, id, rng);
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
        if(gc.canProduceRobot(this.id, UnitType.Knight)) {
            gc.produceRobot(this.id, UnitType.Knight);
        }
        VecUnitID garrison = unit.structureGarrison();
        if(garrison.size() > 0) {
            int nUnloaded = 0;
            for(int i = 0; i < 8; i ++) {
                Direction unloadDir = Utils.getRandomDirection(Direction.values(), this.rng);
                if(gc.canUnload(this.id, unloadDir)) {
                    nUnloaded ++;
                    gc.unload(this.id, unloadDir);
                }
            }
            this.unloadFailed = (nUnloaded == 0);
        } else {
            this.unloadFailed = false;
        }
    }
    
    public boolean wasUnableToUnload() {
        return this.unloadFailed;
    }
}