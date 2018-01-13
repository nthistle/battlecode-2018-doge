import bc.*;
import java.util.Random;

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
}