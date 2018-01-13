import bc.*;
import java.util.Random;

public abstract class UnitHandler {

    protected final GameController gc;
    protected final Random rng;
    protected final int id;

    public UnitHandler(GameController gc, int id, Random rng) {
        this.gc = gc;
        this.id = id;
        this.rng = rng;
    }
    
    // subclasses required to override; if they prefer to use the
    // method that takes unit as a parameter, this may be replaced
    // with simply a call to that with gc.unit(id) as the argument
    public abstract void takeTurn();
    
    // subclasses can optionally override this if they want 
    // more convenient access to the unit (rather than gc.unit(id))
    // this will be called by player in the loop
    public void takeTurn(Unit unit) {
        this.takeTurn();
    }
}