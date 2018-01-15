import bc.*;
import java.util.Random;
import java.util.HashMap;

public class MarsController extends PlanetController
{
    public MarsController(GameController gc, Random rng) {
        super(gc, rng);
    }
    
    public void control() {
    
        System.out.println("Mars Controller initiated");
        
        while (true) {
            gc.nextTurn();
        }
    }
    
    public Planet getPlanet() {
        return Planet.Mars;
    }

    public int getRobotCount(UnitType type) {
        return 0;
    }

    public void incrementRobotCount(UnitType type) {
        return;
    }
}