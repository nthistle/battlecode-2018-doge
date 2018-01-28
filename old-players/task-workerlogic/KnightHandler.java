import bc.*;
import java.util.Random;

public class KnightHandler extends UnitHandler {

    public KnightHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }
    
    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        

    @Override
    public void takeTurn(Unit unit) {
        if (unit.location().isOnMap()) {
            Utils.tryMoveRotate(gc, unit, unit.location().mapLocation().directionTo(((EarthController)parent).targetLocation));            
        }
    }
}