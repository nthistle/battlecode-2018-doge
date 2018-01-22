import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public class Utils
{
    public static Direction getRandomDirection(Direction[] dirs, Random rng) {
        return dirs[rng.nextInt(dirs.length)];
    }
    
    public static Team getOtherTeam(Team thisTeam) {
        if(thisTeam==Team.Blue) return Team.Red;
        return Team.Blue;
    }

    // fisher-yates
    public static Direction[] shuffleDirectionArray(Direction[] array, Random rng) {
        Direction temp;
        int index;
        for (int i = array.length - 1; i > 0; i--)
        {
            index = rng.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
        return array;
    }

    // sort of like a dot product
    public static int getDirectionAffinity(Direction a, Direction b) {
        int adx = bc.bcDirectionDx(a);
        int ady = bc.bcDirectionDy(a);
        int bdx = bc.bcDirectionDx(b);
        int bdy = bc.bcDirectionDy(b);
        if(adx==0||ady==0) {
            adx*=2; ady*=2;
        }
        if(bdx==0||bdy==0) {
            bdx*=2; bdy*=2;
        }
        return (adx*bdx)+(ady*bdy);
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

    public static Direction reverseDirection(Direction dir) {
        switch(dir) {
            case North: return Direction.South;
            case Northwest: return Direction.Southeast;
            case West: return Direction.East;
            case Southwest: return Direction.Northeast;
            case South: return Direction.North;
            case Southeast: return Direction.Northwest;
            case East: return Direction.West;
            case Northeast: return Direction.Southwest;
            default: break;
        }
        return Direction.Center;
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
    
    public static boolean tryMoveWiggleRecur(GameController gc, int unitID, Direction dir, Set<Integer> visited) {
        if(!gc.isMoveReady(unitID) || dir == Direction.Center) return false;
        if(visited != null && visited.contains(unitID)) return false;
        if(gc.canMove(unitID, dir)) {
            gc.moveRobot(unitID, dir);
            return true;
        }
        else {
            //attempt straight-on recursion
            MapLocation myLocation = gc.unit(unitID).location().mapLocation();
            if(visited == null) visited = new HashSet<Integer>();
            visited.add(unitID);
            if(gc.hasUnitAtLocation(myLocation.add(dir))) {
                // System.out.println("Trying to recur move in " + dir);
                Unit bih = gc.senseUnitAtLocation(myLocation.add(dir));
                // System.out.println("before: " + bih);
                if(Utils.tryMoveWiggleRecur(gc, bih.id(), dir, visited)) {
                    // System.out.println("after: " + bih);
                    gc.moveRobot(unitID, dir);
                    // System.out.println("Success!");
                    return true;
                }
            }
            
            //attempt moving to adjacencies
            Direction[] adj = getRotateDirections(dir);
            for(Direction c : adj) {
                // System.out.println("Re-trying to recur move in " + c);
                if(gc.hasUnitAtLocation(myLocation.add(c))) {
                    if(Utils.tryMoveWiggleRecur(gc, gc.senseUnitAtLocation(myLocation.add(c)).id(), c, visited)) {
                        // System.out.println("Success!");
                        gc.moveRobot(unitID, c);
                        return true;
                    }
                }
            }
            return false;
        }
    }
    
    // tries to move in dir, if fail, trys two adjacent dirs
    // returns 0 if move failed, 1 if original dir succeeded,
    // 2 if counter-clockwise neighbor succeeded,
    // 3 if clockwise neighbor succeeded
    public static int tryMoveWiggle(GameController gc, int unitId, Direction dir) {
        if(!gc.isMoveReady(unitId)) {
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

    public static boolean compareMapLocation(MapLocation a, MapLocation b) {
        return a.getPlanet() == b.getPlanet() && a.getX() == b.getX() && a.getY() == b.getY();
    }

    public static boolean canMoveWiggle(GameController gc, int unitId, Direction dir) {
        Direction[] neighboring = getAdjacentDirs(dir);
        return gc.canMove(unitId, dir) || gc.canMove(unitId, neighboring[0]) || gc.canMove(unitId, neighboring[1]);
    }

    public static ArrayList<Direction> directionList = new ArrayList<Direction>(Arrays.asList(Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest));

    public static int[] smallRotation = new int[] {0, -1, 1};
    public static int[] mediumRotation = new int[] {0, -1, 1, -2, 2};
    public static int[] getRotation = new int[] {-1, 1, -2, 2};
    public static int[] bigRotation = new int[] {0, -1, 1, -2, 2, -3, 3, 4};

    public static Direction[] getRotateDirections(Direction direction) {
        Direction[] directions = new Direction[4];
        int index = directionList.indexOf(direction);
        for (int i = 0; i < getRotation.length; i++) {
            directions[i] = directionList.get((8 + index + getRotation[i]) % 8);
        }
        return directions;   
    }

    public static boolean tryMoveRotate(GameController gc, int id, Direction direction) {
        if (!gc.isMoveReady(id)) {
            return false;   
        }
        int index = directionList.indexOf(direction);
        for (int i = 0; i < mediumRotation.length; i++) {
            Direction tryDirection = directionList.get((8 + index + mediumRotation[i]) % 8);
            if (gc.canMove(id, tryDirection)) {
                gc.moveRobot(id, tryDirection);
                return true;
            }
        }
        return false;
    }

    public static boolean tryReplicateRotate(GameController gc, int id, Direction direction) {
        int index = directionList.indexOf(direction);
        for (int i = 0; i < bigRotation.length; i++) {
            Direction tryDirection = directionList.get((8 + index + bigRotation[i]) % 8);
            if (gc.canReplicate(id, tryDirection)) {
                gc.replicate(id, tryDirection);
                return true;
            }
        }
        return false;
    }


//    public static boolean canOccupy(GameController gc, MapLocation location, PlanetController parent, HashSet<MapLocation> visited) {
//        if (visited.contains(location)) {
//            return true;
//        }
//        PlanetMap map = ((EarthController)parent).map;                
//        boolean status = map.onMap(location) && map.isPassableTerrainAt(location) == 1 && !gc.hasUnitAtLocation(location);
//        if (status) {
//            visited.add(location);
//        }
//        return status;
//    }
//
//    public static boolean canOccupy(GameController gc, MapLocation location, PlanetController parent, UnitType type, HashSet<MapLocation> visited) {
//        if (visited.contains(location)) {
//            return true;
//        }
//        PlanetMap map = ((EarthController)parent).map;
//        boolean status = !map.onMap(location) || map.isPassableTerrainAt(location) == 0 || !gc.hasUnitAtLocation(location) || gc.senseUnitAtLocation(location).unitType() != type;
//        if (status) {
//            visited.add(location);
//        }
//        return status;
//    }
    
    public static boolean canOccupyMars(GameController gc, MapLocation location) {
        PlanetMap map = gc.startingMap(Planet.Mars);
        boolean status = map.onMap(location) && map.isPassableTerrainAt(location) == 1 && !gc.hasUnitAtLocation(location);
        return status;
    }
}