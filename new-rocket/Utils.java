import bc.*;
import java.util.*;

public class Utils
{
    public static Direction getRandomDirection(Direction[] dirs, Random rng) {
        return dirs[rng.nextInt(dirs.length)];
    }
    
    public static Team getOtherTeam(Team thisTeam) {
        if(thisTeam==Team.Blue) return Team.Red;
        return Team.Blue;
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

    public static Direction[] directions() {
        return new Direction[] {
        	Direction.North,
        	Direction.East, 
        	Direction.South,
        	Direction.West,
        	Direction.Northeast,
        	Direction.Southeast,
        	Direction.Southwest,
        	Direction.Northwest
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
    

    public static long distanceSquaredTo(MapLocation a, MapLocation b) {
        return ((long)Math.pow((a.getX() - b.getX()), 2)) + ((long)Math.pow((a.getY() - b.getY()), 2));
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
    			System.out.println("Trying to recur move in " + dir);
    			Unit bih = gc.senseUnitAtLocation(myLocation.add(dir));
    			System.out.println("before: " + bih);
    			if(Utils.tryMoveWiggleRecur(gc, bih.id(), dir, visited)) {
    				System.out.println("after: " + bih);
    				gc.moveRobot(unitID, dir);
    				System.out.println("Success!");
    				return true;
    			}
    		}
    		
    		//attempt moving to adjacencies
    		Direction[] adj = Utils.getAdjacentDirs(dir);
    		for(Direction c : adj) {
    			System.out.println("Re-trying to recur move in " + c);
    			if(gc.hasUnitAtLocation(myLocation.add(c))) {
    				if(Utils.tryMoveWiggleRecur(gc, gc.senseUnitAtLocation(myLocation.add(c)).id(), c, visited)) {
    					System.out.println("Success!");
    					gc.moveRobot(unitID, c);
    					return true;
    				}
    			}
    		}
    		return false;
    	}
    }

    public static boolean compareMapLocation(MapLocation a, MapLocation b) {
        return a.getPlanet() == b.getPlanet() && a.getX() == b.getX() && a.getY() == b.getY();
    }

    public static int canMoveWiggle(GameController gc, int unitId, Direction dir) {
        if(gc.canMove(unitId, dir)) {
            return 1;
        }
        Direction[] neighboring = getAdjacentDirs(dir);
        if(neighboring == null)
            return 0;
        if(gc.canMove(unitId, neighboring[0])) {
            return 2;
        }
        if(gc.canMove(unitId, neighboring[1])) {
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

    public static boolean canOccupy(GameController gc, MapLocation location, PlanetController parent, HashSet<MapLocation> visited) {
        if (visited.contains(location)) {
            return true;
        }
        PlanetMap map = ((EarthController)parent).earthMap;
        boolean status = map.onMap(location) && map.isPassableTerrainAt(location) == 1 && !gc.hasUnitAtLocation(location);
        if (status) {
            visited.add(location);
        }
        return status;
    }

    public static boolean canOccupy(GameController gc, MapLocation location, PlanetController parent, UnitType type, HashSet<MapLocation> visited) {
        if (visited.contains(location)) {
            return true;
        }
        PlanetMap map = ((EarthController)parent).earthMap;
        boolean status = !map.onMap(location) || map.isPassableTerrainAt(location) == 0 || !gc.hasUnitAtLocation(location) || gc.senseUnitAtLocation(location).unitType() != type;
        if (status) {
            visited.add(location);
        }
        return status;
    }
    
    public static boolean canOccupyMars(GameController gc, MapLocation location) {
    	PlanetMap map = gc.startingMap(Planet.Mars);
    	boolean status = map.onMap(location) && map.isPassableTerrainAt(location) == 0 && !gc.hasUnitAtLocation(location);
    	return status;
    }
}