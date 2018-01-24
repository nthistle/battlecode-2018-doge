import bc.*;

import java.util.*;

public class LaunchingLogicHandler extends UnitHandler  {
	protected List<ArrayList<MapLocation>> zoneMap; 
	protected MapLocation firstContactLandingPoint;
	protected MapLocation fightingTroopLandingPoint;
	protected static List<MapLocation> usedLandingPoints;
	protected long launchingTime;
	protected static int[][] values;
	protected static int[][] label;
	protected static int[][] adjacentSquares;
	protected static List<Integer> kryptoniteTotals;
	protected static List<Integer> usedZones;
	public LaunchingLogicHandler(PlanetController parent, GameController gc, int id, Random rng) {
		super(parent, gc, id, rng);
        this.zoneMap = this.getZones();
        System.out.println(Arrays.deepToString(label));
        System.out.println(this.zoneMap);
        this.usedLandingPoints = new ArrayList<MapLocation>();
	}
	
	public void takeTurn() {
		recalculate();
	}

	public void recalculate() {
		calculateOptimalLandingLocationFirstContact();
		calculateOptimalLandingLocationFightingTroops();
		calculateOptimalLaunchingTime();
	}
	
	public MapLocation optimalLandingLocation(boolean firstContact) {
		if(firstContact) {
			return this.firstContactLandingPoint;
		}
		else {
			return this.fightingTroopLandingPoint;
		}
	}
	
	public MapLocation optimalFirstContactLandingLocation() {
		return this.firstContactLandingPoint;
	}
	
	public MapLocation optimalFightingTroopLandingLocation() {
		return this.fightingTroopLandingPoint;
	}
	
	public long optimalLaunchingTime() {
		return this.launchingTime;
	}
	
	private MapLocation calculateOptimalLandingLocationFirstContact() {
		Collections.sort(this.zoneMap, Comparators.VecMapLocComp);
		for(int i = 0; i < this.zoneMap.size(); i++) {
			if(usedZones.get(i) > 0) continue;
			else {
				for(MapLocation spot : this.zoneMap.get(i)) {
					if(!usedLandingPoints.contains(spot)) {
						this.firstContactLandingPoint = spot;
						return spot;
					}
				}
			}
		}
		return this.firstContactLandingPoint = this.calculateOptimalLandingLocationFightingTroops();
	}
	
	private MapLocation calculateOptimalLandingLocationFightingTroops() {
		Collections.sort(this.zoneMap, Comparators.VecMapLocComp);
		for(int i = 0; i < this.zoneMap.size(); i++) {
			if(usedZones.get(i) == 0) continue;
			else {
				for(MapLocation spot : this.zoneMap.get(i)) {
					if(!usedLandingPoints.contains(spot)) {
						this.fightingTroopLandingPoint = spot;
						return spot;
					}
				}
			}
		}
		return this.fightingTroopLandingPoint = this.calculateOptimalLandingLocationFirstContact();
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
		int i = ml.getY(), j = ml.getX();
		for(int di = -1; di <= 1; di++) {
			for(int dj = -1; dj <= 1; dj++) {
				try {
					adjacentSquares[i+di][j+dj]--;
				}catch(Exception e){}
			}
		}
		usedZones.set(label[i][j]-1, usedZones.get(label[i][j]-1) + 1);
		recalculate();
	}
	
	private List<ArrayList<MapLocation>> getZones() {
		PlanetMap marsMap = gc.startingMap(Planet.Mars);
		System.out.println(marsMap.getHeight());
		System.out.println(marsMap.getWidth());
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
				System.out.println(i + ", " + j);
				if(!Utils.canOccupyMars(gc, new MapLocation(Planet.Mars, j, i))) {
					label[i][j] = -1; //impassable point
				}
				else if(label[i][j] == 0){ //not yet visited
					zone++;
					recur(label, i, j, zone, marsMap);
				}
				for(int di = -1; di <= 1; di++) {
					for(int dj = -1; dj <= 1; dj++) {
						try {
							if(label[i+di][j+dj] > 0) adjacentSquares[i][j]++;
						}catch(Exception e) {}
					}
				}
				if(marsMap.isPassableTerrainAt(new MapLocation(Planet.Mars, j, i)) != 0) 
					values[i][j] += (int)marsMap.initialKarboniteAt(new MapLocation(Planet.Mars, j, i));
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
				|| !Utils.canOccupyMars(gc, new MapLocation(Planet.Mars, j, i))) 
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
				int totA = kryptoniteTotals.get(label[(int)a.get(0).getY()][(int)a.get(0).getX()]-1);
				int totB = kryptoniteTotals.get(label[(int)b.get(0).getY()][(int)b.get(0).getX()]-1);
				return (totB + 100 * b.size() - 100 * usedZones.get(label[(int)b.get(0).getY()][(int)b.get(0).getX()]-1)) - (totA + 100 * a.size() - 100 * usedZones.get(label[(int)a.get(0).getY()][(int)a.get(0).getX()]-1));
			}
		};
		
		public static Comparator<MapLocation> MapLocComp = new Comparator<MapLocation>() {
			public int compare(MapLocation a, MapLocation b) {
				for(MapLocation loc : usedLandingPoints) {
					if(Utils.compareMapLocation(a, loc)) return 1; 
					if(Utils.compareMapLocation(b, loc)) return -1;
				}
				return (10 * adjacentSquares[b.getY()][b.getX()] - kryptoniteTotals.get(label[a.getY()][a.getX()]-1)) - (10 * adjacentSquares[a.getY()][a.getX()] - kryptoniteTotals.get(label[a.getY()][a.getX()]-1));
			}
		};
	}
	
	        
}