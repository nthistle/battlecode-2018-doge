import bc.*;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;

public class Player {
    public static void main(String[] args) {        

        GameController gc = new GameController();

        Direction[] directions = Direction.values();

        while (true) {

            VecUnit units = gc.myUnits();
            for (int i = 0; i < units.size(); i++) {
                Unit unit = units.get(i);

                if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
                    gc.moveRobot(unit.id(), Direction.Southeast);
                }
            }

            gc.nextTurn();
        }
    }
}