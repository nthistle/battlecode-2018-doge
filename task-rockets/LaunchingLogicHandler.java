import bc.*;
import java.util.Random;
import java.util.Set;

public class LaunchingLogicHandler extends UnitHandler implements Comparator<ArrayList<MapLocation>> {
	private List<LinkedList<MapLocation>> zoneMap; 
	private MapLocation landingPoint;
	private List<MapLocation> usedLandingPoints;
	private long launchingTime;
	public LaunchingLogicHandler(PlanetController parent, GameController gc, int id, Random rng) {
		this.parent = parent;
        this.gc = gc;
        this.id = id;
        this.rng = rng;
        this.zoneMap = this.getZones();
	}
	
	public void takeTurn() {
		
	}
	
	public MapLocation optimalLandingLocation() {
		
	}
	
	public long optimalLaunchingTime() {
		
	}
	
	public getZones() {
		PlanetMap marsMap = gc.startingMap(Planet.Mars);
		int[][] values = new int[marsMap.getHeight()][marsMap.getWidth()];
		int[][] label = new int[marsMap.getHeight()][marsMap.getWidth()];
		zone = 0;
		AsteroidPattern asPat = gc.asteroidPattern();
		for(int i = 0; i < 1000; i++) {
			if(asPat.hasAsteroid(i)) {
				AsteroidStrike strike = asPat.asteroid(i);
				values[strike.getLocation().getY()][strike.getLocation().getX()] += strike.getKarbonite();
			}
		}
		for(int i = 0; i < marsMap.getHeight(); i++) {
			for(int j = 0; j < marsMap.getWidth(); j++) {
				if(!gc.isOccupiable(new MapLocation(Planet.Mars, j, i))) {
					label[i][j] = -1; //impassable point
				}
				else if(label[i][j] == 0){ //not yet visited
					zone++;
					recur(label, i, j, zone, marsMap);
				}
			}
		}
		List<ArrayList<MapLocation>> ret = new ArrayList<ArrayList<MapLocation>>(zone);
		for(i = 0; i < zones; i++) {
			ret.add(new ArrayList<MapLocation>());
		}
		for(int i = 0; i < marsMap.getHeight(); i++) {
			for(int j = 0; j < marsMap.getWidth(); j++) {
				if(label[i][j] > 0) {
					ret.get(label[i][j] - 1).add(new MapLocation(Planet.Mars, j, i));
				}
			}
		}
		return ret;
	}
	
	private void recur(int[][] label, int i, int j, int tag, PlanetMap marsMap) {
		if(i < 0
				|| i >= marsMap.getHeight()
				|| j < 0
				|| j >= marsMap.getWidth()
				|| label[i][j] != 0
				|| !gc.isOccupable(new MapLocation(Planet.Mars, j, i))) 
			return;
		else {
			label[i][j] = tag;
			recur(label, i+1, j, tag, marsMap);
			recur(label, i+1, j+1, tag, marsMap);
			recur(label, i, j+1, tag, marsMap);
			recur(label, i-1, j+1, tag, marsMap);
			recur(label, i-1, j, tag, marsMap);
			recur(label, i-1, j-1, tag, marsMap);
			recur(label, i, j-1, tag, marsMap);
			recur(label, i+1, j-1, tag, marsMap);
		}
	}
	
	private int compare(ArrayList<MapLocation> a, ArrayList<MapLocation> b) {
		
	}
}