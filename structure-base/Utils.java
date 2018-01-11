import bc.*;
import java.util.Random;

public class Utils
{
    public static Direction getRandomDirection(Direction[] dirs, Random rng) {
        return dirs[rng.nextInt(dirs.length)];
    }
    
    public static Team getOtherTeam(Team thisTeam) {
        if(thisTeam==Team.Blue) return Team.Red;
        return Team.Blue;
    }
    
    // lazy way
    public static Direction[] getAdjacentDirs(Direction dir) {
        switch(dir) {
          case Direction.North:
            return new Direction[] {Direction.Northwest, Direction.Northeaest};
          case Direction.Northeast:
            return new Direction[] {Direction.North, Direction.East};
          case Direction.East:
            return new Direction[] {Direction.Northeast, Direction.Southeast};
          case Direction.Southeast:
            return new Direction[] {Direction.East, Direction.South};
          case Direction.South:
            return new Direction[] {Direction.Southeast, Direction.Southwest};
          case Direction.Southwest:
            return new Direction[] {Direction.South, Direction.West};
          case Direction.West:
            return new Direction[] {Direction.Southwest, Direction.Northwest};
          case Direction.Northwest:
            return new Direction[] {Direction.West, Direction.North};
          default:
            break;
        }
        return null;
    }
    
    // tries to move in dir, if fail, trys two adjacent dirs
    // returns 0 if move failed, 1 if original dir succeeded,
    // 2 if counter-clockwise neighbor succeeded,
    // 3 if clockwise neighbor succeeded
    public static int tryMoveWiggle(GameController gc, int unitId, Direction dir) {
        if(gc.canMove(unitId, dir)) {
            gc.moveRobot(unitId, dir);
            return 1;
        }
        Direction[] neighboring = getAdjacentDirs(dir);
        if(gc.canMove(unitId, neighboring[0])) {
            gc.moveRobot(unitId, neighboring[0]);
            return 2;
        }
        if(gc.canMove(unitId, neighboring[1])) {
            gc.moveRobot(unitId, neighboring[1]);
            return 3;
        }
        return 0;
    }
}