import bc.*;

public class Bug {

	private GameController gc;
	private int id;

	private PlanetMap map;
		
	private MapLocation mapLocation = null;	
	private MapLocation destLocation = null;
	private Direction directDirection = null;	
	private int directDistance = Integer.MAX_VALUE;

	private MapLocation wallLocation = null;	
	private boolean buggingRight = true; 

	private int smoothCount = 0;
	private int closestDistance = Integer.MAX_VALUE;

	private boolean[][] visitedLocations = null;	
	private int width = 0;
	private int height = 0;

	private boolean isTracing = false;	

	public Bug(GameController gc, int id, PlanetMap map) {
		this.gc = gc;
		this.id = id;				
		this.map = map;
		width = (int)this.map.getWidth();
		height = (int)this.map.getHeight();
	}

	public void bugMove(MapLocation start, MapLocation dest) {
		if (!gc.isMoveReady(id) || start.equals(dest)) {
			return;
		}
		if (destLocation != null && !dest.equals(destLocation)) {			
			isTracing = false;
		}

		mapLocation = start;
		destLocation = dest;
		directDirection = mapLocation.directionTo(destLocation);
		directDistance = (int)mapLocation.distanceSquaredTo(destLocation);

		if (isTracing) {
			if (directDistance < closestDistance && Utils.trySmallMoveRotate(gc, id, directDirection)) {
				isTracing = false;
				return;
			}
			traceMove();
		} else if (!Utils.trySmallMoveRotate(gc, id, directDirection)) {
			smoothCount = Integer.MAX_VALUE;
			closestDistance = directDistance;
			visitedLocations = new boolean[height][width];
			isTracing = true;		
			
			Direction leftDirection = bc.bcDirectionRotateLeft(directDirection);
			int leftDistance = Integer.MAX_VALUE;
			for (int i = 0; i < 3; i++) {
				leftDirection = bc.bcDirectionRotateLeft(leftDirection);
				if (gc.canMove(id, leftDirection)) {
					leftDistance = (int)mapLocation.add(leftDirection).distanceSquaredTo(destLocation);
					break;
				}
			}
			Direction rightDirection = bc.bcDirectionRotateRight(directDirection);
			int rightDistance = Integer.MAX_VALUE;
			for (int i = 0; i < 2; i++) {		
				rightDirection = bc.bcDirectionRotateRight(rightDirection);
				if (gc.canMove(id, rightDirection)) {
					rightDistance = (int)mapLocation.add(rightDirection).distanceSquaredTo(destLocation);
					break;
				}
			}

			if (rightDistance < leftDistance) {
				buggingRight = true;
				wallLocation = mapLocation.add(bc.bcDirectionRotateLeft(rightDirection));
			} else {
				buggingRight = false;
				wallLocation = mapLocation.add(bc.bcDirectionRotateRight(leftDirection));
			}

			traceMove();
		}
	}

	private void traceMove() {
		traceMove(false);
		if (smoothCount >= 2) {
			isTracing = false;
		}
	}
	
	private void traceMove(boolean reversed) {
		Direction tryDirection = mapLocation.directionTo(wallLocation);
		visitedLocations[mapLocation.getX() % height][mapLocation.getY() % width] = true;
		if (gc.canMove(id, tryDirection)) {
			smoothCount += 1;
		} else {
			smoothCount = 0;
		}
		for (int i = 0; i < 8; i++) {
			if (buggingRight) {
				tryDirection = bc.bcDirectionRotateRight(tryDirection);
			} else {
				tryDirection = bc.bcDirectionRotateLeft(tryDirection);
			}
			MapLocation tryLocation = mapLocation.add(tryDirection);
			if (gc.hasUnitAtLocation(tryLocation) && !reversed) {
				buggingRight = !buggingRight;
				traceMove(true);
				return;				
			}
			if (!map.onMap(tryLocation) && !reversed) {
				buggingRight = !buggingRight;
				traceMove(true);
				return;
			}
			if (gc.canMove(id, tryDirection)) {
				gc.moveRobot(id, tryDirection);				
				if (visitedLocations[tryLocation.getX() % height][tryLocation.getY() % width]) {
					isTracing = false;
				}
				return;
			}
			wallLocation = tryLocation;			
		}
	}	
}