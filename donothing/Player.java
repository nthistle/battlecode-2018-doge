import bc.*;
import java.util.Random;

public class Player {
    
    public static void main(String[] args) {
        
        GameController gc = new GameController();

        while(true) {
            gc.takeTurn();
        }
    }
}