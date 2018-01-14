import bc.*;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

public class LaunchingLogicHandler extends UnitHandler  {
	private List<LinkedList<MapLocation>> zoneMap; 
	private MapLocation landingPoint;
	private List<MapLocation> usedLandingPoints;
	private long launchingTime;
	private[][] int values;
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
	
	private void calculateOptimalLandingLocation() {
		Collections.sort(this.zoneMap, this.VecMapLocComp);
		
	}
	
	private void calculateOptimalLaunchingTime() {
		
	}
	
	private getZones() {
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
		this.values = values;
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
	
	private static class VecMapLocComp implements Comparator<ArrayList<MapLocation>> {
		private int compare(ArrayList<MapLocation> a, ArrayList<MapLocation> b) { //select the objectively better zone
			
			int totA = 0; totB = 0;
			for(MapLocation loc : a) {
				totA += this.values[loc.getY()][loc.getX()];
			}
			for(MapLocation loc : b) {
				totB += this.values[loc.getY()][loc.getX()];
			}
			return (0.01 * totB + b.size()) - (0.01 * totA + a.size()); //adjust the coeffs to see 
		}
	}
	
	private static class MapLocComp implements Comparator<MapLocation> {
		private int compare(MapLocation a, MapLocation b) {
			return this.values[a.getY()][a.getX()] - this.values[b.getY()][b.getX()];
		}
	}
	
	
}