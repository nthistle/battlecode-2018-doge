import bc.*;
import java.util.*;
import java.awt.Point;

public class CommunicationsManager {
	private MarsController parent;
	private Random rng;
	private GameController gc;
	private Team myTeam;
	private Team enemyTeam;
	private Set<Point> loggedPoints;
	public CommunicationsManager(MarsController parent, GameController gc, Random rng) {
		this.parent = parent;
		this.rng = rng;
		this.gc = gc;
		this.myTeam = gc.team();
		this.enemyTeam = Utils.getOtherTeam(this.myTeam);
		this.loggedPoints = new HashSet<Point>();
	}
	
	public void update() {
		//clear the array
		System.out.println(gc.getTeamArray(Planet.Mars).toString());
		
		for(int i = 0; i < 100; i++) {
			gc.writeTeamArray(i, 0);
		}
		
		VecUnit myTroops = gc.senseNearbyUnitsByTeam(new MapLocation(Planet.Mars, 0, 0), Long.MAX_VALUE, myTeam);
		VecUnit enemyTroops = gc.senseNearbyUnitsByTeam(new MapLocation(Planet.Mars, 0, 0), Long.MAX_VALUE, enemyTeam);
		
		int[] myCounts = new int[6];
		int[] enemyCounts = new int[6];
		
		for(int i = 0; i < myTroops.size(); i++) {
			Unit unit = myTroops.get(i);
			if(unit.unitType() == UnitType.Worker) myCounts[0]++;
			else if(unit.unitType() == UnitType.Knight) myCounts[1]++;
			else if(unit.unitType() == UnitType.Ranger) myCounts[2]++;
			else if(unit.unitType() == UnitType.Mage) myCounts[3]++;
			else if(unit.unitType() == UnitType.Healer) myCounts[4]++;
			else if(unit.unitType() == UnitType.Rocket) myCounts[5]++;
		}
		
		for(int i = 0; i < enemyTroops.size(); i++) {
			Unit unit = enemyTroops.get(i);
			if(unit.unitType() == UnitType.Worker) enemyCounts[0]++;
			else if(unit.unitType() == UnitType.Knight) enemyCounts[1]++;
			else if(unit.unitType() == UnitType.Ranger) enemyCounts[2]++;
			else if(unit.unitType() == UnitType.Mage) enemyCounts[3]++;
			else if(unit.unitType() == UnitType.Healer) enemyCounts[4]++;
			else if(unit.unitType() == UnitType.Rocket) enemyCounts[5]++;
		}
		
		for(int i = 0; i < 6; i++) {
			gc.writeTeamArray(i, myCounts[i]);
			gc.writeTeamArray(6+i, enemyCounts[i]);
		}
		
		VecUnit rockets = gc.senseNearbyUnitsByType(new MapLocation(Planet.Mars, 0, 0), Long.MAX_VALUE, UnitType.Rocket);
		Set<Point> visitedPoints = new HashSet<Point>();
		int i = 12;
		for(int j = 0; j < rockets.size(); j++) {
			if(i >= 100) return;
			Unit rocket = rockets.get(j);
			MapLocation loc = rocket.location().mapLocation();
			Point p = new Point(loc.getX(), loc.getY());
			Team t = rocket.team();
			visitedPoints.add(p);
			if(loggedPoints.contains(p)) continue;
			gc.writeTeamArray(i, (t == myTeam ? 10000 : 20000) + 100 * p.x + 1 * p.y);
			loggedPoints.add(p);
			i++;
		}
		Set<Point> removedPoints = new HashSet<Point>(loggedPoints);
		removedPoints.removeAll(visitedPoints);
		if(removedPoints.size() == 0) return;
		for(Point p : removedPoints) {
			if(i >= 100) return;
			gc.writeTeamArray(i, 30000 + 100 * p.x + 1 * p.y);
			i++;
		}
		
	}
}