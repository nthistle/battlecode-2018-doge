import bc.*;
import java.util.Random;

public abstract class UnitHandler {

    private GameController gc;
    private Random rng;

    public UnitHandler(GameController gc, Random rng) {
        this.gc = gc;
        this.rng = rng;
    }
    
    public abstract void takeTurn();
}