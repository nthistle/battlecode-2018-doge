import bc.*;

import java.util.*;

public class LaunchingLogicHandler extends UnitHandler  {
	protected List<ArrayList<MapLocation>> zoneMap; 
	protected MapLocation landingPoint;
	protected static List<MapLocation> usedLandingPoints;
	protected long launchingTime;
	protected static int[][] values;
	public LaunchingLogicHandler(PlanetController parent, GameController gc, int id, Random rng) {
		super(parent, gc, id, rng);
        this.zoneMap = this.getZones();
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
		ArrayList<MapLocation> optimalZone = this.zoneMap.get(0);
		Collections.sort(optimalZone, Comparators.MapLocComp);
		this.landingPoint = optimalZone.get(0);
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
		int[][] values = new int[(int)marsMap.getHeight()][(int)marsMap.getWidth()];
		int[][] label = new int[(int)marsMap.getHeight()][(int)marsMap.getWidth()];
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
		List<ArrayList<MapLocation>> ret = new ArrayList<ArrayList<MapLocation>>(zone);
		for(int i = 0; i < zone; i++) {
			ret.add(new ArrayList<MapLocation>());
		}
		for(int i = 0; i < marsMap.getHeight(); i++) {
			for(int j = 0; j < marsMap.getWidth(); j++) {
				if(label[i][j] > 0) {
					ret.get(label[i][j] - 1).add(new MapLocation(Planet.Mars, j, i));
				}
			}
		}
		this.values = values;
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
				int totA = 0, totB = 0;
				for(MapLocation loc : a) {
					totA += values[loc.getY()][loc.getX()];
				}
				for(MapLocation loc : b) {
					totB += values[loc.getY()][loc.getX()];
				}
				return (totB + 100 * b.size()) - (totA + 109 * a.size()); //adjust the coeffs to see 
			}
		};
		
		public static Comparator<MapLocation> MapLocComp = new Comparator<MapLocation>() {
			public int compare(MapLocation a, MapLocation b) {
				for(MapLocation loc : usedLandingPoints) {
					if(Utils.compareMapLocation(a, loc)) return 1; 
					if(Utils.compareMapLocation(b, loc)) return -1;
				}
				return values[a.getY()][a.getX()] - values[b.getY()][b.getX()];
			}
		};
	}
	
	
}