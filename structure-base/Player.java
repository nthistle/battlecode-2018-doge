import bc.*;
import java.util.Random;

public class Player {

    private static final String playerName = "Structure Base";
    private static final int seed = -1; // -1 for no seed
    
    public static void main(String[] args) {
    
        Random rand;
        if(seed == -1) rand = new Random();
        else rand = new Random(seed);
        
        GameController gc = new GameController();
        
        if(gc.planet() == Planet.Earth) {
            new EarthController(gc, rand).control();
        } else if(gc.planet() == Planet.Mars) {
            new MarsController(gc, rand).control();
        }
    }
}