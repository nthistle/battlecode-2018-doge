import bc.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Iterator;
import java.util.Arrays;

public class Cluster {

	public final Point clusterMaxima;
	public List<Point> members;
	public List<Point> frontier;
	public MiningMaster m;
	public int minersAt = 0;
	public Point maxPoint;

	//don't hate me for this
	public int id = new Random().nextInt(10);

	public Cluster(MiningMaster m, Point a) {
		this.clusterMaxima = a;
		this.members = new ArrayList<Point>();
		this.frontier = new ArrayList<Point>();
		this.m = m;
		this.frontier.add(a);
	}

	public void addToCluster(Point a) {
		members.add(a);
	}

	public boolean expandCluster() {
		List<Point> newFrontier = new ArrayList<Point>();
		for(Point p : this.frontier) {
			if(this.m.clusterMap[p.x][p.y] == null) {
				this.members.add(p);
				this.m.clusterMap[p.x][p.y] = this;
				int[][] initialDirections = {new int[] {0, 1}, new int[] {0, -1}, new int[] {1, 0}, new int[] {1, 1}, new int[] {1, -1}, new int[] {-1, 0}, new int[] {-1, -1}, new int[] {-1, 1}};
				for(int[] a : initialDirections) {
					if(p.x+a[0] < this.m.initialKarboniteLocations.length && p.x+a[0] >= 0 && p.y+a[1] < this.m.initialKarboniteLocations[0].length && p.y+a[1] >= 0 && this.m.initialKarboniteLocations[p.x+a[0]][p.y+a[1]] >= MiningMaster.KARBONITE_THRESHOLD_CLUSTER && this.m.initialKarboniteLocations[p.x+a[0]][p.y+a[1]] < this.m.initialKarboniteLocations[p.x][p.y]) {
						newFrontier.add(new Point(p.x+a[0], p.y+a[1]));
					}
				}
			}
		}
		if(this.frontier.size() == 0) {
			this.frontier = newFrontier;
			return false;
		}
		this.frontier = newFrontier;
		return true;
	}

	public void update(Point location, int minedAmount) {
		Iterator<Point> it = this.members.iterator();
		this.maxPoint = this.members.get(0);
		while(it.hasNext()) {
			Point curr = it.next();
			if(location.equals(curr)) {
				int base = m.initialKarboniteLocationsOriginal[location.x][location.y];
				if(base - minedAmount <= 0) {
					m.initialKarboniteLocationsOriginal[location.x][location.y] = 0;
					it.remove();
					continue;
				} else {
					m.initialKarboniteLocationsOriginal[location.x][location.y] -= minedAmount;
				}
			}
			if(m.initialKarboniteLocationsOriginal[curr.x][curr.y] > m.initialKarboniteLocationsOriginal[this.maxPoint.x][this.maxPoint.y]) {
				this.maxPoint = new Point(curr.x, curr.y);
			}
		}
		//System.out.println(Arrays.toString(this.members.toArray()));
		//System.out.println(this.maxPoint);
		if(this.members.size() == 0) {
			System.out.println("We don't have anything else in this cluster at [" + clusterMaxima.x + "," + clusterMaxima.y + "]");
			this.maxPoint = null;
		}
	}

	public static int heuristic(Cluster a) {
		MapLocation nearestTeam = null;
        int nearestDist = Integer.MAX_VALUE;
		for(int i = 0; i < a.m.teamStartingPositions.size(); i++) {
			if(nearestTeam == null || Cluster.distanceSquaredTo(a.clusterMaxima, new Point(a.m.teamStartingPositions.get(i).getX(), a.m.teamStartingPositions.get(i).getY())) < nearestDist) {
                nearestTeam = a.m.teamStartingPositions.get(i);
                nearestDist = (int) Cluster.distanceSquaredTo(a.clusterMaxima, new Point(a.m.teamStartingPositions.get(i).getX(), a.m.teamStartingPositions.get(i).getY()));
            }
		}
		return nearestDist * -1000 + a.members.size() * 2 + Cluster.value(a) * 1000; 
	}

	private static int value(Cluster a) {
		int val = 0;
		for(Point b : a.members) {
			val += a.m.initialKarboniteLocationsOriginal[b.x][b.y];
		}
		return val;
	}

	//helper method
	public static long distanceSquaredTo(Point a, Point b) {
		return ((long)Math.pow((a.getX() - b.getX()), 2)) + ((long)Math.pow((a.getY() - b.getY()), 2));
	}

}