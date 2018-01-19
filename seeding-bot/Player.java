import bc.*;
import java.util.Random;

public class Player {

    private static final String playerName = "Structure Base";
    private static final int seed = -1; // -1 for no seed
    
    public static void main(String[] args) {
    
        System.out.println(playerName + " initiated");
    
        Random rand;
        if(seed == -1) rand = new Random();
        else rand = new Random(seed);
        
        GameController gc = new GameController();

        PathMaster pm = new PathMaster(gc.startingMap(gc.planet()));
        
        PlanetController pc = null;
        if(gc.planet() == Planet.Earth) {
            pc = new EarthController(gc, pm, rand);
        } else if(gc.planet() == Planet.Mars) {
            pc = new MarsController(gc, pm, rand);
        }
        
        System.out.println("Running PlanetController...");
        pc.control();
    }
}