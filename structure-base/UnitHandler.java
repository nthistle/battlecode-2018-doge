import bc.*;
import java.util.Random;

public abstract class UnitHandler {

    private GameController gc;
    private Random rng;
    private final int id;

    public UnitHandler(GameController gc, int id, Random rng) {
        this.gc = gc;
        this.id = id;
        this.rng = rng;
    }
    
    public abstract void takeTurn();
}