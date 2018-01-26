import bc.*;
import java.util.*;
import java.awt.Point;

public class LaunchingLogicHandler extends UnitHandler  {
	protected List<ArrayList<MapLocation>> zoneMap; 
	protected MapLocation optimalLandingLocation;
	protected static Set<Point> usedLandingPoints;
	protected long launchingTime;
	protected static int[][] values;
	protected static int[][] label;
	protected static int[][] adjacentSquares;
	protected static int[][] rocketDistances;
	protected static List<Integer> kryptoniteTotals;
	protected static List<Integer> usedZones;
	protected int rocketsLoaded = 0;
	protected PlanetMap marsMap;

	public LaunchingLogicHandler(PlanetController parent, GameController gc, int id, Random rng) {
		super(parent, gc, id, rng);
		this.marsMap = gc.startingMap(Planet.Mars);
        this.zoneMap = this.getZones();
        //System.out.println(Arrays.deepToString(label));
        //System.out.println(this.zoneMap);
        this.usedLandingPoints = new HashSet<Point>();

	}
	
	public void takeTurn() {
		recalculate();
	}

	public void recalculate() {
		calculateOptimalLandingLocation();
		calculateOptimalLaunchingTime();
	}
	
	public MapLocation optimalLandingLocation() {
		return this.optimalLandingLocation;
	}
	
	public long optimalLaunchingTime() {
		return this.launchingTime;
	}
	
	private MapLocation calculateOptimalLandingLocation() {
		Collections.sort(this.zoneMap, Comparators.VecMapLocComp);
		for(ArrayList<MapLocation> area : this.zoneMap) {
			Collections.sort(area, Comparators.MapLocComp);
			for(MapLocation spot : area) {
				if(!usedLandingPoints.contains(new Point(spot.getX(), spot.getY()))) {
					this.optimalLandingLocation = spot;
					return spot;
				}
			}
		}
		return null;
	}
	
	private void calculateOptimalLaunchingTime() {
		long arrivalTime = gc.round() + gc.currentDurationOfFlight();
		for(long i = gc.round() + 1; i < 1000; i++) {
			if(i + gc.currentDurationOfFlight() < arrivalTime) {
				arrivalTime = gc.round() + gc.currentDurationOfFlight();
			}
			else {
				this.launchingTime = i-1;
				return;
			}
		}
	}
	
	public double[] nextManifest() {
		this.rocketsLoaded++;
		if(this.rocketsLoaded < Math.max(3, this.zoneMap.size() / 10)) return RocketHandler.FIRST_CONTACT_CREW;
		else if(this.rocketsLoaded % 4 == 0) return RocketHandler.FIRST_CONTACT_CREW;
		else return RocketHandler.RANGER_CREW;
	}
	
	public void addUsedMapLocation(MapLocation ml) {
		this.usedLandingPoints.add(new Point(ml.getX(), ml.getY()));
		int y = ml.getY(), x = ml.getX();
		for(int di = -1; di <= 1; di++) {
			for(int dj = -1; dj <= 1; dj++) {
				try {
					adjacentSquares[y+di][x+dj]--;
				}catch(Exception e){}
			}
		}
		for(int i = 0; i < this.marsMap.getHeight(); i++) {
			for(int j = 0; j < this.marsMap.getWidth(); j++) {
				rocketDistances[i][j] += (y-i) * (y-i) + (x-j) * (x-j);
			}
		}
		rocketsLoaded++;
		usedZones.set(label[y][x]-1, usedZones.get(label[y][x]-1) + 1);
		recalculate();
	}
	
	private List<ArrayList<MapLocation>> getZones() {
		//System.out.println(this.marsMap.getHeight());
		//System.out.println(this.marsMap.getWidth());
		values = new int[(int)this.marsMap.getHeight()][(int)this.marsMap.getWidth()];
		label = new int[(int)this.marsMap.getHeight()][(int)this.marsMap.getWidth()];
		rocketDistances = new int[(int)this.marsMap.getHeight()][(int)this.marsMap.getWidth()];
		adjacentSquares = new int[(int)this.marsMap.getHeight()][(int)this.marsMap.getWidth()];
		int zone = 0;
		AsteroidPattern asPat = gc.asteroidPattern();
		for(int i = 0; i < 1000; i++) {
			if(asPat.hasAsteroid(i)) {
				AsteroidStrike strike = asPat.asteroid(i);
				values[(int)strike.getLocation().getY()][(int)strike.getLocation().getX()] += strike.getKarbonite();
			}
		}
		for(int i = 0; i < this.marsMap.getHeight(); i++) {
			for(int j = 0; j < this.marsMap.getWidth(); j++) {
				//System.out.println(i + ", " + j);
				if(!Utils.canOccupyMars(gc, new MapLocation(Planet.Mars, j, i))) {
					label[i][j] = -1; //impassable point
				}
				else if(label[i][j] == 0){ //not yet visited
					zone++;
					recur(label, i, j, zone);
				}
				for(int di = -1; di <= 1; di++) {
					for(int dj = -1; dj <= 1; dj++) {
						try {
							if(label[i+di][j+dj] > 0) adjacentSquares[i][j]++;
						}catch(Exception e) {}
					}
				}
				if(this.marsMap.isPassableTerrainAt(new MapLocation(Planet.Mars, j, i)) != 0) 
					values[i][j] += (int)this.marsMap.initialKarboniteAt(new MapLocation(Planet.Mars, j, i));
			}
		}
		List<ArrayList<MapLocation>> ret = new ArrayList<ArrayList<MapLocation>>(zone);
		kryptoniteTotals = new ArrayList<Integer>(zone);
		usedZones = new ArrayList<Integer>(zone);
		for(int i = 0; i < zone; i++) {
			ret.add(new ArrayList<MapLocation>());
			kryptoniteTotals.add(0);
			usedZones.add(0);
		}
		for(int i = 0; i < this.marsMap.getHeight(); i++) {
			for(int j = 0; j < this.marsMap.getWidth(); j++) {
				if(label[i][j] > 0) {
					ret.get(label[i][j] - 1).add(new MapLocation(Planet.Mars, j, i));
					kryptoniteTotals.set(label[i][j]-1, kryptoniteTotals.get(label[i][j]-1) + values[i][j]);
				}
			}
		}
		return ret;
	}
	
	private void recur(int[][] label, int i, int j, int tag) {
		if(i < 0
				|| i >= this.marsMap.getHeight()
				|| j < 0
				|| j >= this.marsMap.getWidth()
				|| label[i][j] != 0
				|| !Utils.canOccupyMars(gc, new MapLocation(Planet.Mars, j, i))) 
			return;
		else {
			label[i][j] = tag;
			recur(label, i+1, j, tag);
			recur(label, i+1, j+1, tag);
			recur(label, i, j+1, tag);
			recur(label, i-1, j+1, tag);
			recur(label, i-1, j, tag);
			recur(label, i-1, j-1, tag);
			recur(label, i, j-1, tag);
			recur(label, i+1, j-1, tag);
		}
	}
	
	public static class Comparators {
		public static Comparator<ArrayList<MapLocation>> VecMapLocComp = new Comparator<ArrayList<MapLocation>>() {
			public int compare(ArrayList<MapLocation> a, ArrayList<MapLocation> b) { //select the objectively better zone
				int totA = kryptoniteTotals.get(label[(int)a.get(0).getY()][(int)a.get(0).getX()]-1);
				int totB = kryptoniteTotals.get(label[(int)b.get(0).getY()][(int)b.get(0).getX()]-1);
				return (totB + 100 * b.size() - 100 * usedZones.get(label[(int)b.get(0).getY()][(int)b.get(0).getX()]-1)) - (totA + 100 * a.size() - 100 * usedZones.get(label[(int)a.get(0).getY()][(int)a.get(0).getX()]-1));
			}
		};
		
		public static Comparator<MapLocation> MapLocComp = new Comparator<MapLocation>() {
			public int compare(MapLocation a, MapLocation b) {
				int i1 = a.getY(), i2 = b.getY(), j1 = a.getX(), j2 = b.getX();
				return (1000 * adjacentSquares[i2][j2] - values[i2][j2] - (int)(0.01 * rocketDistances[i2][j2])) - (1000 * adjacentSquares[i1][j1] - values[i1][j1] - (int)(0.01 * rocketDistances[i1][j1]));			}
		};
	}
	
	        
}