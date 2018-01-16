import bc.*;
import java.util.Random;

public class MarsController extends PlanetController
{
    public MarsController(GameController gc, PathMaster pm, Random rng) {
        super(gc, pm, rng);
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

    @Override
    public void requestSwarm(int goalSize, MapLocation target, UnitType a) {
        return;
    }
}