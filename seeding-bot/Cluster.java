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

	public void setMaxPoint(Point a) {
		this.maxPoint = a;
	}

	public Point getMaxPoint() {
		return this.maxPoint;
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


	//returns true if there is still karbonite left in the cluster, returns false if there is no more karbonite left in the cluster
	public boolean update(Point location, int minedAmount) {
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
			Iterator<Cluster> clusterIt = m.clusters.iterator();
			while(clusterIt.hasNext()) {
				Cluster q = clusterIt.next();
				if(q.clusterMaxima.equals(this.clusterMaxima))
					clusterIt.remove();
			}
			this.m.parentController.pm.clearPFCache(new MapLocation(this.m.parentController.getPlanet(), this.clusterMaxima.x, this.clusterMaxima.y));
			for(int i = 0; i < this.m.clusters.size(); i++) {
				Cluster q = this.m.clusters.get(i);
				if(this.m.parentController.pm.getCachedPathField(q.clusterMaxima.x, q.clusterMaxima.y) == null) {
					MapLocation end = new MapLocation(this.m.parentController.getPlanet(), q.clusterMaxima.x, q.clusterMaxima.y);
					this.m.parentController.pm.getPathFieldWithCache(end);
					break;
				}
			}
			return false;
		}
		return true;
	}

	public int value() {
		return Cluster.value(this);
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

		return nearestDist * -10 + a.members.size() * 2 + (int) Math.pow(Cluster.value(a), 2);
		//return a.members.size() * 2 + (int) Math.pow(Cluster.value(a), 2); 
	}

	public static int value(Cluster a) {
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