import bc.*;
import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;

public class Utils
{
    public static Direction getRandomDirection(Direction[] dirs, Random rng) {
        return dirs[rng.nextInt(dirs.length)];
    }

    public static Direction[] shuffleDirectionArray(Direction[] array, Random random) {
        Direction temp;
        int index;
        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
        return array;
    }
    
    public static Team getOtherTeam(Team thisTeam) {
        if(thisTeam==Team.Blue) return Team.Red;
        return Team.Blue;
    }

    public static Direction[] directions() {
        return new Direction[] {
            Direction.North,
            Direction.Northwest,
            Direction.West,
            Direction.Southwest,
            Direction.South,
            Direction.Southeast,
            Direction.East,
            Direction.Northeast
        };
    }
    
    // lazy way
    public static Direction[] getAdjacentDirs(Direction dir) {
        switch(dir) {
          case North:
            return new Direction[] {Direction.Northwest, Direction.Northeast};
          case Northeast:
            return new Direction[] {Direction.North, Direction.East};
          case East:
            return new Direction[] {Direction.Northeast, Direction.Southeast};
          case Southeast:
            return new Direction[] {Direction.East, Direction.South};
          case South:
            return new Direction[] {Direction.Southeast, Direction.Southwest};
          case Southwest:
            return new Direction[] {Direction.South, Direction.West};
          case West:
            return new Direction[] {Direction.Southwest, Direction.Northwest};
          case Northwest:
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
        if(!gc.isMoveReady(unitId) || dir == Direction.Center) {
            return 0;
        }
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

    public static ArrayList<Direction> directionList = new ArrayList<Direction>(Arrays.asList(Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest));

    public static int[] smallRotation = new int[] {0, -1, 1};
    public static int[] bigRotation = new int[] {0, -1, 1, -2, 2};

    public static int tryMoveRotate(GameController gc, Unit unit, Direction direction) {
        if (!gc.isMoveReady(unit.id())) {
            return -1;   
        }
        int index = directionList.indexOf(direction);
        for (int i = 0; i < bigRotation.length; i++) {
            Direction tryDirection = directionList.get((8 + index + bigRotation[i]) % 8);
            if (gc.canMove(unit.id(), tryDirection)) {
                gc.moveRobot(unit.id(), tryDirection);
                return i;
            }
        }
        return -1;
    }


    public static boolean compareMapLocation(MapLocation a, MapLocation b) {
        return a.getPlanet() == b.getPlanet() && a.getX() == b.getX() && a.getY() == b.getY();
    }

    public static boolean canMoveWiggle(GameController gc, int unitId, Direction dir) {
        Direction[] neighboring = getAdjacentDirs(dir);
        return gc.canMove(unitId, dir) || gc.canMove(unitId, neighboring[0]) || gc.canMove(unitId, neighboring[1]);
    }
}