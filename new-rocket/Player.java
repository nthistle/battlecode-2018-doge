import bc.*;
import java.util.Random;
import java.util.ArrayList;

public class Player {

    private static final String playerName = "Sprint Bot";
    private static final int seed = -1; // -1 for no seed
    
    public static void main(String[] args) {
    
        System.out.println(playerName + " initiated");
    
        Random rand;
        if(seed == -1) rand = new Random();
        else rand = new Random(seed);
        
        GameController gc = new GameController();
        
        PlanetController pc = null;
        if(gc.planet() == Planet.Earth) {
            pc = new EarthController(gc, new PathMaster(gc.startingMap(Planet.Earth)), rand);
        } else if(gc.planet() == Planet.Mars) {
            pc = new MarsController(gc, new PathMaster(gc.startingMap(Planet.Mars)), rand);
        }
        
        System.out.println("Running PlayerController...");
        pc.control();
    }
}