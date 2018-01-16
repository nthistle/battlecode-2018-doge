import bc.*;

import java.util.*;

public class LaunchingLogicHandler extends UnitHandler  {
	protected List<ArrayList<MapLocation>> zoneMap; 
	protected MapLocation landingPoint;
	protected static List<MapLocation> usedLandingPoints;
	protected long launchingTime;
	protected static int[][] values;
	protected static int[][] label;
	protected static int[][] adjacentSquares;
	protected static List<Integer> kryptoniteTotals;
	public LaunchingLogicHandler(PlanetController parent, GameController gc, int id, Random rng) {
		super(parent, gc, id, rng);
        this.zoneMap = this.getZones();
        System.out.println(label);
        this.usedLandingPoints = new ArrayList<MapLocation>();
	}
	
	public void takeTurn() {
		calculateOptimalLandingLocation();
		calculateOptimalLaunchingTime();
	}
	
	public MapLocation optimalLandingLocation() {
		return this.landingPoint;
	}
	
	public long optimalLaunchingTime() {
		return this.launchingTime;
	}
	
	private void calculateOptimalLandingLocation() {
		Collections.sort(this.zoneMap, Comparators.VecMapLocComp);
		for(ArrayList<MapLocation> zone : this.zoneMap) {
			Collections.sort(zone, Comparators.MapLocComp);
			for(MapLocation spot : zone) {
				if(!usedLandingPoints.contains(spot)) this.landingPoint = spot;
			}
		}
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
	
	public void addUsedMapLocation(MapLocation ml) {
		this.usedLandingPoints.add(ml);
	}
	
	private List<ArrayList<MapLocation>> getZones() {
		PlanetMap marsMap = gc.startingMap(Planet.Mars);
		values = new int[(int)marsMap.getHeight()][(int)marsMap.getWidth()];
		label = new int[(int)marsMap.getHeight()][(int)marsMap.getWidth()];
		adjacentSquares = new int[(int)marsMap.getHeight()][(int)marsMap.getWidth()];
		int zone = 0;
		AsteroidPattern asPat = gc.asteroidPattern();
		for(int i = 0; i < 1000; i++) {
			if(asPat.hasAsteroid(i)) {
				AsteroidStrike strike = asPat.asteroid(i);
				values[(int)strike.getLocation().getY()][(int)strike.getLocation().getX()] += strike.getKarbonite();
			}
		}
		for(int i = 0; i < marsMap.getHeight(); i++) {
			for(int j = 0; j < marsMap.getWidth(); j++) {
				if(gc.isOccupiable(new MapLocation(Planet.Mars, j, i)) == 0) {
					label[i][j] = -1; //impassable point
				}
				else if(label[i][j] == 0){ //not yet visited
					zone++;
					recur(label, i, j, zone, marsMap);
				}
			}
		}
		for(int i = 0; i < marsMap.getHeight(); i++) {
			for(int j = 0; j < marsMap.getWidth(); j++) {
				for(int di = -1; di <= 1; di++) {
					for(int dj = -1; dj <= 1; dj++) {
						if(label[i+di][j+dj] > 0) adjacentSquares[i][j]++;
					}
				}
			}
		}
		List<ArrayList<MapLocation>> ret = new ArrayList<ArrayList<MapLocation>>(zone);
		kryptoniteTotals = new ArrayList<Integer>(zone);
		for(int i = 0; i < zone; i++) {
			ret.add(new ArrayList<MapLocation>());
			kryptoniteTotals.add(0);
		}
		for(int i = 0; i < marsMap.getHeight(); i++) {
			for(int j = 0; j < marsMap.getWidth(); j++) {
				if(label[i][j] > 0) {
					ret.get(label[i][j] - 1).add(new MapLocation(Planet.Mars, j, i));
					kryptoniteTotals.set(label[i][j]-1, kryptoniteTotals.get(label[i][j]-1) + values[i][j]);
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
				|| gc.isOccupiable(new MapLocation(Planet.Mars, j, i)) == 0) 
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
	
	public static class Comparators {
		public static Comparator<ArrayList<MapLocation>> VecMapLocComp = new Comparator<ArrayList<MapLocation>>() {
			public int compare(ArrayList<MapLocation> a, ArrayList<MapLocation> b) { //select the objectively better zone
				int totA = kryptoniteTotals.get(label[(int)a.get(0).getY()][(int)a.get(0).getX()]);
				int totB = kryptoniteTotals.get(label[(int)b.get(0).getY()][(int)b.get(0).getX()]);
				return (totB + 100 * b.size()) - (totA + 100 * a.size());
			}
		};
		
		public static Comparator<MapLocation> MapLocComp = new Comparator<MapLocation>() {
			public int compare(MapLocation a, MapLocation b) {
				for(MapLocation loc : usedLandingPoints) {
					if(Utils.compareMapLocation(a, loc)) return 1; 
					if(Utils.compareMapLocation(b, loc)) return -1;
				}
				return (10 * adjacentSquares[b.getY()][b.getX()] - kryptoniteTotals.get(label[a.getY()][a.getX()])) - (10 * adjacentSquares[a.getY()][a.getX()] - kryptoniteTotals.get(label[a.getY()][a.getX()]));
			}
		};
	}
	
	
}