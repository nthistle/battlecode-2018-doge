import bc.*;
import java.util.*;
import java.awt.Point;

public class LaunchingLogicHandler extends UnitHandler  {
	protected List<ArrayList<MapLocation>> zoneMap; 
	protected MapLocation optimalLandingLocation;
	protected static Set<Point> myUsedLandingPoints;
	protected static Set<Point> enemyUsedLandingPoints;
	protected long launchingTime;
	protected static int[][] values;
	protected static int[][] label;
	protected static int[][] adjacentSquares;
	protected static int[][] rocketDistances;
	protected static List<Integer> kryptoniteTotals;
	protected static List<Integer> usedZones;
	protected static List<Integer> enemyUsedZones;
	protected static Team enemyTeam;
	protected int rocketsLoaded = 0;
	protected PlanetMap marsMap;
	protected int[] myMarsTroops;
	protected int[] enemyMarsTroops;

	public LaunchingLogicHandler(PlanetController parent, GameController gc, int id, Random rng) {
		super(parent, gc, id, rng);

        this.enemyTeam = ((EarthController) parent).enemyTeam;
		this.marsMap = gc.startingMap(Planet.Mars);
        this.zoneMap = this.getZones();
        //System.out.println(Arrays.deepToString(label));
        //System.out.println(this.zoneMap);
        this.myUsedLandingPoints = new HashSet<Point>();
        this.enemyUsedLandingPoints = new HashSet<Point>();
        this.myMarsTroops = new int[6];
        this.enemyMarsTroops = new int[6];

	}
	
	public void takeTurn() {
		recieveCommunications();
		recalculate();
	}

	public void recalculate() {
		calculateOptimalLandingLocation();
		calculateOptimalLaunchingTime();
	}
	
	public void recieveCommunications() {
		Veci32 data = gc.getTeamArray(Planet.Mars);
		for(int i = 0; i < 6; i++) {
			this.myMarsTroops[i] = data.get(i);
			this.enemyMarsTroops[i] = data.get(i+6);
		}
		for(int i = 12; i <= 100; i++) {
			int deet = data.get(i);
			if(deet == 0) return;
			int status = deet / 10000;
			int x = (deet / 100) % 100;
			int y = deet % 100;
			System.out.println("Status: " + status + ", X: " + x + ", Y: " + y);
			if(status == 1) { //new friendly location
				this.addUsedMapLocation(new MapLocation(Planet.Mars, x, y));
			}
			else if(status == 2) { //new enemy location
				this.addEnemyUsedMapLocation(new MapLocation(Planet.Mars, x, y));
			}
			else if(status == 3){ //destroyed rocket  
				Point p = new Point(x, y);
				if(myUsedLandingPoints.contains(p)) {
					this.removeUsedMapLocation(new MapLocation(Planet.Mars, x, y));
				}
				else if(enemyUsedLandingPoints.contains(p)) {
					this.removeEnemyUsedMapLocation(new MapLocation(Planet.Mars, x, y));
				}
			}
		}
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
				if(!myUsedLandingPoints.contains(new Point(spot.getX(), spot.getY())) && 
						!enemyUsedLandingPoints.contains(new Point(spot.getX(), spot.getY()))) {
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
		if(myMarsTroops[0] < 8) 
			return RocketHandler.FIRST_CONTACT_CREW; //low workers
		else if(myMarsTroops[2] > 2 * enemyMarsTroops[2]) 
			return RocketHandler.MAGE_HEALER_CREW; //send in mages
		else if(myMarsTroops[2] > 1.5 * myMarsTroops[4])
			return RocketHandler.RANGER_HEALER_CREW_3;
		else if(myMarsTroops[2] < myMarsTroops[4]) 
			return RocketHandler.RANGER_HEALER_CREW_2;
		else
			return RocketHandler.RANGER_HEALER_CREW_1;
			
	}
	
	public void addUsedMapLocation(MapLocation ml) {
		if(!this.myUsedLandingPoints.add(new Point(ml.getX(), ml.getY()))) return;
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
				rocketDistances[i][j] += 5 * (y-i) * (y-i) + 5 * (x-j) * (x-j);
			}
		}
		rocketsLoaded++;
		usedZones.set(label[y][x]-1, usedZones.get(label[y][x]-1) + 1);
		recalculate();
	}
	
	private void removeUsedMapLocation(MapLocation ml) {
		this.myUsedLandingPoints.remove(new Point(ml.getX(), ml.getY()));
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
				rocketDistances[i][j] -= 10 * (y-i) * (y-i) + 10 * (x-j) * (x-j);
			}
		}
		rocketsLoaded--;
		usedZones.set(label[y][x]-1, usedZones.get(label[y][x]-1) - 1);
		recalculate();
	}
	
	private void addEnemyUsedMapLocation(MapLocation ml) {
		if(!this.enemyUsedLandingPoints.add(new Point(ml.getX(), ml.getY()))) return;
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
				rocketDistances[i][j] += 4 * (y-i) * (y-i) + 4 * (x-j) * (x-j);
			}
		}
		enemyUsedZones.set(label[y][x]-1, enemyUsedZones.get(label[y][x]-1) + 1);
		recalculate();
	}
	
	private void removeEnemyUsedMapLocation(MapLocation ml) {
		this.enemyUsedLandingPoints.remove(new Point(ml.getX(), ml.getY()));
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
				rocketDistances[i][j] -= 4 * (y-i) * (y-i) + 4 * (x-j) * (x-j);
			}
		}
		enemyUsedZones.set(label[y][x]-1, enemyUsedZones.get(label[y][x]-1) - 1);
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
		for(int i = enemyTeam == Team.Red ? (int)(this.marsMap.getHeight() - 1) : 0; enemyTeam == Team.Red ? i >= 0: i < this.marsMap.getHeight(); i += enemyTeam == Team.Red ? -1 : 1) {
			for(int j = enemyTeam == Team.Red ? (int)(this.marsMap.getWidth() - 1) : 0; enemyTeam == Team.Red ? j >= 0: j < this.marsMap.getWidth(); j += enemyTeam == Team.Red ? -1 : 1) {
				//System.out.println(i + ", " + j);
				MapLocation ml = new MapLocation(Planet.Mars, j, i);
				if(marsMap.onMap(ml) && marsMap.isPassableTerrainAt(ml) == 0) {
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
				if(this.marsMap.isPassableTerrainAt(ml) != 0) 
					values[i][j] += (int)this.marsMap.initialKarboniteAt(ml);
			}
		}
		List<ArrayList<MapLocation>> ret = new ArrayList<ArrayList<MapLocation>>(zone);
		kryptoniteTotals = new ArrayList<Integer>(zone);
		usedZones = new ArrayList<Integer>(zone);
		enemyUsedZones = new ArrayList<Integer>(zone);
		for(int i = 0; i < zone; i++) {
			ret.add(new ArrayList<MapLocation>());
			kryptoniteTotals.add(0);
			usedZones.add(0);
			enemyUsedZones.add(0);
		}
		for(int i = enemyTeam == Team.Red ? (int)(this.marsMap.getHeight() - 1) : 0; enemyTeam == Team.Red ? i >= 0: i < this.marsMap.getHeight(); i += enemyTeam == Team.Red ? -1 : 1) {
			for(int j = enemyTeam == Team.Red ? (int)(this.marsMap.getWidth() - 1) : 0; enemyTeam == Team.Red ? j >= 0: j < this.marsMap.getWidth(); j += enemyTeam == Team.Red ? -1 : 1) {
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
				int comp = (totB + 100 * b.size() - 100 * usedZones.get(label[(int)b.get(0).getY()][(int)b.get(0).getX()]-1) + 400 * enemyUsedZones.get(label[(int)b.get(0).getY()][(int)b.get(0).getX()]-1)) 
						- (totA + 100 * a.size() - 100 * usedZones.get(label[(int)a.get(0).getY()][(int)a.get(0).getX()]-1) + 400 * enemyUsedZones.get(label[(int)a.get(0).getY()][(int)a.get(0).getX()]-1));
				return comp;
			}
		};
		
		public static Comparator<MapLocation> MapLocComp = new Comparator<MapLocation>() {
			public int compare(MapLocation a, MapLocation b) {
				int i1 = a.getY(), i2 = b.getY(), j1 = a.getX(), j2 = b.getX();
				int comp =  (10 * adjacentSquares[i2][j2] - values[i2][j2] - (int)(0.01 * rocketDistances[i2][j2])) 
						- (10 * adjacentSquares[i1][j1] - values[i1][j1] - (int)(0.01 * rocketDistances[i1][j1]));
				return comp;
			}
		};
	}

    public void handleDeath() {} 
}
