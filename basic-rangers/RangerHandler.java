import bc.*;
import java.util.Random;

public class RangerHandler {

    public RangerHandler(GameController gc, int id, Random rng) {
        super(gc, id, rng);
    }
    
    public void takeTurn() {
        // for movement, we're going to use "push forces"
        // enemies that are too close push us away, enemies that are closer to outer shooting range
        // will draw us closer, and allies lightly repel us (causes spread / advance towards enemy)
        double xForce = 0.0;
        double yForce = 0.0;
        
    }
}