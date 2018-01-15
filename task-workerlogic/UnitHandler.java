import bc.*;
import java.util.Random;

public abstract class UnitHandler {

    protected final PlanetController parent;
    protected final GameController gc;
    protected final Random rng;
    protected final int id;    

    public UnitHandler(PlanetController parent, GameController gc, int id, Random rng) {
        this.parent = parent;
        this.gc = gc;
        this.id = id;
        this.rng = rng;
    }
    
    public abstract void takeTurn();
    
    public void takeTurn(Unit unit) {
        this.takeTurn();
    }
}