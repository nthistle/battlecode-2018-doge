import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.HashSet;
import java.awt.Point;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;


public class MiningMaster {

	protected int[][] initialKarboniteLocations;
	protected int[][] initialKarboniteLocationsOriginal;
	protected List<Cluster> clusters;
	private int[] miningWorkerHandlers;
	public static final int KARBONITE_THRESHOLD = 5;
	public static final int KARBONITE_THRESHOLD_CLUSTER = 2;
	public static final int MAX_CLUSTERS_VISIT = 3;
	public static final int MAX_MINERS_AT_CLUSTER = 3;
	public static final int MIN_CLUSTER_VALUE = 15;
	public static final int MAX_PATHFIELDS = 10;
	protected Cluster[][] clusterMap;
	protected PlanetController parentController;

	//
	protected List<MapLocation> teamStartingPositions;
	//

	public MiningMaster(PlanetController pc) {
		this.parentController = pc;
		generateVariables();
	}

	private void generateVariables() {
		this.teamStartingPositions = new ArrayList<MapLocation>();
		VecUnit original = this.parentController.gc.startingMap(this.parentController.getPlanet()).getInitial_units();
		for(int i = 0; i < original.size(); i++) {
            if(original.get(i).team() == this.parentController.gc.team()) {
                this.teamStartingPositions.add(original.get(i).location().mapLocation());
            }
        }
        System.out.println("We intially have " + this.teamStartingPositions.size() + " workers on the field.");
	}

	private void generateIntialKarboniteLocations() {
		PlanetMap initialMap = this.parentController.gc.startingMap(this.parentController.getPlanet());
		this.initialKarboniteLocations = new int[(int) initialMap.getWidth()][(int) initialMap.getHeight()];
		this.initialKarboniteLocationsOriginal = new int[(int) initialMap.getWidth()][(int) initialMap.getHeight()];
		this.clusterMap = new Cluster[initialKarboniteLocations.length][initialKarboniteLocations[0].length];
		for(int i = 0; i < this.initialKarboniteLocations.length; i++) {
			for(int j = 0; j < this.initialKarboniteLocations[0].length; j++) {
				int base =((int) initialMap.initialKarboniteAt(new MapLocation(this.parentController.getPlanet(), i, j)));
				if(base > 0) {
					this.initialKarboniteLocations[i][j] = base;
					this.initialKarboniteLocationsOriginal[i][j] = base;
				}
			}
		}
	}


	//return true if you should mine, next step would be to call convertToMiner(int id) with the unit id of the worker you want to start mining with
	public boolean shouldMine() {
		int clustersFound = 0;
		for(int i = 0; i < this.clusters.size(); i++) {
			Cluster q = this.clusters.get(i);
			if(this.parentController.pm.getCachedPathField(q.clusterMaxima.x, q.clusterMaxima.y) != null) {
				int minDist = Integer.MAX_VALUE;
				for(int j = 0; j < this.teamStartingPositions.size(); j++) {
					MapLocation end = new MapLocation(this.parentController.getPlanet(), q.clusterMaxima.x, q.clusterMaxima.y);
					int dist = Integer.MAX_VALUE;
					if(this.parentController.pm.getCachedPathField(q.clusterMaxima.x, q.clusterMaxima.y) != null) {
						dist = this.parentController.pm.getPathFieldWithCache(end).getDistanceAtPoint(this.teamStartingPositions.get(j));
					} else {
						dist = Integer.MAX_VALUE;
					}
					if(dist < minDist)
						minDist = dist;
				}
				if(minDist <= 25 && Cluster.value(q) >= 50)
					return true;
				else if(minDist <= 10 && Cluster.value(q) >= 25)
					clustersFound++;
			}
		}
		if(clustersFound >= 3)
			return true;
		return false;
	}

	private void generateMaximas() {
		this.clusters = new ArrayList<Cluster>();
		int[][] initialDirections = {new int[] {0, 1}, new int[] {0, -1}, new int[] {1, 0}, new int[] {1, 1}, new int[] {1, -1}, new int[] {-1, 0}, new int[] {-1, -1}, new int[] {-1, 1}};
		for(int i = 0; i < initialKarboniteLocations.length; i++) {
			for(int j = 0; j < initialKarboniteLocations[0].length; j++) {
				if(initialKarboniteLocations[i][j]<KARBONITE_THRESHOLD) continue;
				boolean isMaxima = true;
				for(int[] a : initialDirections) {
					//if we are within boundary but one of the neighbors are greater than us, we are not a maxima
					if(i+a[0] < initialKarboniteLocations.length && i+a[0] >= 0 && j+a[1] < initialKarboniteLocations[0].length && j+a[1] >= 0 && initialKarboniteLocations[i][j] < initialKarboniteLocations[i+a[0]][j+a[1]])
						isMaxima = false;
				}
				if(isMaxima) {
					//lets decrement all the neighbors of this maxima
					this.clusters.add(new Cluster(this, new Point(i, j)));
					for(int[] a : initialDirections) {
						if(i+a[0] < initialKarboniteLocations.length && i+a[0] >= 0 && j+a[1] < initialKarboniteLocations[0].length && j+a[1] >= 0) {
							if(this.initialKarboniteLocations[i+a[0]][j+a[1]] != 0) {
								this.initialKarboniteLocations[i+a[0]][j+a[1]] -= 1;
							}
						}
					}
				}
			}
		}
		//System.out.println("Clusters Size: " + this.clusters.size());
	}

	public void printKarboniteMap() {
		for(int i = 0; i < initialKarboniteLocations.length; i++) {
			for(int j = 0; j < initialKarboniteLocations[0].length; j++) {
				String a = initialKarboniteLocations[i][j] + "";
				if(initialKarboniteLocations[i][j]>9)
					System.out.print(a + "   ");
				else
					System.out.print(a + "    ");
				//for(int z = 0; z < 3-a.length(); z++) {
				//	a = " " + a; 
				//}
				//System.out.print(a + " ");
			}
			System.out.println();
		}
	}

	//given the id of preferrably a worker, it will reassign that a minerworkerhandler
	//returns true if the new miner has a target, return false if he doesnt
	public boolean convertToMiner(int id) {
		UnitHandler newHandler = new MiningWorkerHandler(this.parentController, this.parentController.gc, id, this.parentController.rng, this);
		((EarthController) this.parentController).myHandler.put(id, newHandler);
		return assignTarget((MiningWorkerHandler) newHandler);
	}

	//given a mining worker handler, checks if it has a target, if not give it a target
	//returns true if a target was assigned, returns false if not (if false, we dont need any more miners)
	public boolean assignTarget(MiningWorkerHandler a) {
		//TODO fix this so we don't do this by exception
		MapLocation current;
		try {
			current = this.parentController.gc.unit(a.id).location().mapLocation();
		} catch (Exception e) {
			current = null;
		}
		if(current == null)
			return false;

		if(a.hasTarget())
			return true;

		List<Cluster> needToBeFilled = new ArrayList<Cluster>();
		Cluster goal = null;
		long minDist = Integer.MAX_VALUE;

		//first we need the clusters that are accessible by this miner but only those that have been cached
		for(int i = 0; i < Math.min(this.MAX_PATHFIELDS, this.clusters.size()); i++) {
			Cluster q = this.clusters.get(i);
			if(this.parentController.pm.isConnected(current, new MapLocation(this.parentController.getPlanet(), q.clusterMaxima.x, q.clusterMaxima.y)) && this.parentController.pm.getCachedPathField(q.clusterMaxima.x, q.clusterMaxima.y) != null)
				needToBeFilled.add(this.clusters.get(i));
		}

		List<Cluster> needToBeFilled1 = new ArrayList<Cluster>();
		//next, if one of the clusters we want has 0 miners at it, lets try go there
		for(int i = 0; i < needToBeFilled.size(); i++) {
			if(needToBeFilled.get(i).minersAt == 0) {
				needToBeFilled1.add(needToBeFilled.get(i));
			}
		}

		//if there are none with 0 workers, lets just go to a cluster with less than 3 workers
		if(needToBeFilled1.size() == 0) {
			for(int i = 0; i < needToBeFilled.size(); i++) {
				if(needToBeFilled.get(i).minersAt < this.MAX_MINERS_AT_CLUSTER) {
					needToBeFilled1.add(needToBeFilled.get(i));
				}
			}
		}

		//there are no more clusters that have less than 3 workers at them

		if(this.clusters.size() == 0) {
			System.out.println("No more clusters in cluster list");
			return false;
		}

		if(needToBeFilled1.size() == 0 && needToBeFilled.size() == 0) {
			System.out.println("Picked a target from cluster list");
			Cluster q = this.clusters.get(this.parentController.rng.nextInt(this.clusters.size()));
			a.setTarget(new MapLocation(this.parentController.getPlanet(), q.clusterMaxima.x, q.clusterMaxima.y), this.parentController.pm);
			this.clusterMap[q.clusterMaxima.x][q.clusterMaxima.y].minersAt += 1;
			return true;
		}

		if(needToBeFilled1.size() == 0) {
			a.setTarget(new MapLocation(this.parentController.getPlanet(), needToBeFilled.get(0).clusterMaxima.x, needToBeFilled.get(0).clusterMaxima.y), this.parentController.pm);
			this.clusterMap[needToBeFilled.get(0).clusterMaxima.x][needToBeFilled.get(0).clusterMaxima.y].minersAt += 1;
			return true;
		}

		
		//finally select the one that is the closest pathfinding distance to the location of the miner
		TreeMap<Integer, Cluster> locs = new TreeMap<Integer, Cluster>();
		for(int i = 0; i < needToBeFilled1.size(); i++) {
			//this does straight line distance
			//locs.put(Cluster.distanceSquaredTo(needToBeFilled1.get(i).clusterMaxima, new Point(current.getX(), current.getY())), needToBeFilled1.get(i));
			//this does the pathfinding distance
			Cluster q = needToBeFilled1.get(i);
			MapLocation end = new MapLocation(this.parentController.getPlanet(), q.clusterMaxima.x, q.clusterMaxima.y);
			if(this.parentController.pm.getCachedPathField(q.clusterMaxima.x, q.clusterMaxima.y) != null)
				locs.put(this.parentController.pm.getPathFieldWithCache(end).getDistanceAtPoint(current.getX(), current.getY()), needToBeFilled1.get(i));
		}

		if(locs.keySet().size() > 0) {
			a.setTarget(new MapLocation(this.parentController.getPlanet(), locs.get(locs.firstKey()).clusterMaxima.x, locs.get(locs.firstKey()).clusterMaxima.y), this.parentController.pm);
			this.clusterMap[locs.get(locs.firstKey()).clusterMaxima.x][locs.get(locs.firstKey()).clusterMaxima.y].minersAt += 1;
			return true;
		}
		
		return false;
		//this.clusterMap[this.clusters.get(i).clusterMaxima.x][this.clusters.get(i).clusterMaxima.y].minersAt += 1;
	}

	public void printKarboniteMapWithClusters() {
		boolean[][] isMax = new boolean[initialKarboniteLocations.length][initialKarboniteLocations[0].length];
		for(Cluster c : this.clusters) {
			isMax[c.clusterMaxima.x][c.clusterMaxima.y] = true;
		}
		for(int i = 0; i < initialKarboniteLocations.length; i++) {
			for(int j = 0; j < initialKarboniteLocations[0].length; j++) {
				String a = initialKarboniteLocations[i][j] + "";
				if(isMax[i][j]) {
					a = "M" + this.clusterMap[i][j].id + "  ";
					System.out.print(a + " ");
				} else {
					if(initialKarboniteLocations[i][j]<MiningMaster.KARBONITE_THRESHOLD) {
						System.out.print(a + "    ");
					} else {
						a = "m" + this.clusterMap[i][j].id + "  ";
						System.out.print(a + " ");
						
					}
				}
				//for(int z = 0; z < 3-a.length(); z++) {
				//	a = " " + a; 
				//}
				//System.out.print(a + " ");
			}
			System.out.println();
		}
	}

	public boolean update() {
		Iterator<Cluster> it1 = this.clusters.iterator();
		while(it1.hasNext()) {
			Cluster q = it1.next();
			List<Point> members = q.members;
			Iterator<Point> it = members.iterator();
			q.setMaxPoint(members.get(0));
			while(it.hasNext()) {
				Point p = it.next();
				//TODO figure out if there is a better way to do this
				int karb = -1;
				try {
					karb = (int) this.parentController.gc.karboniteAt(new MapLocation(this.parentController.getPlanet(), p.x, p.y));
				} catch (Exception e) {
					//we can't see this point
				}
				if(karb != -1) {
					if(karb < 1) {
						it.remove();
						this.initialKarboniteLocationsOriginal[p.x][p.y] = 0;
					}
					this.initialKarboniteLocationsOriginal[p.x][p.y] = karb;
				}
				if(this.initialKarboniteLocationsOriginal[p.x][p.y] > this.initialKarboniteLocationsOriginal[q.getMaxPoint().x][q.getMaxPoint().y])
					q.setMaxPoint(p);
			}
			if(q.members.size() == 0) {
				it1.remove();
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	public boolean updateIndividual(Point a, int karb) {
		this.initialKarboniteLocationsOriginal[a.x][a.y] = karb;
		Cluster q = this.clusterMap[a.x][a.y];
		if(q != null) {
			if(karb == 0) {
				q.members.remove(a);
			}
			if(q.members.size() == 0) {
				Iterator<Cluster> i = this.clusters.iterator();
				while(i.hasNext()) {
					if(i.next().clusterMaxima.equals(q.clusterMaxima)) {
						i.remove();
						return true;
					}
				}
			}
		}
		return false;
	}

	public void generate() {
		generateIntialKarboniteLocations();
		//printKarboniteMap();
		generateMaximas();
		//printKarboniteMapWithClusters();
		
		while(true) {
			boolean finished = true;
			for(Cluster c : this.clusters) {
				if(c.expandCluster())
					finished = false;
			}
			if(finished)
				break;
		}
		//printKarboniteMapWithClusters();

		System.out.println("We have " + this.clusters.size() + " clusters of karbonite on the map");

		Collections.sort(this.clusters, Comparators.clusterComparator);

		Iterator<Cluster> it = this.clusters.iterator();
		while(it.hasNext()) {
			Cluster q = it.next();
			boolean isPathConnected = false;
			MapLocation end = new MapLocation(this.parentController.getPlanet(), q.clusterMaxima.x, q.clusterMaxima.y);
			for(int j = 0; j < this.teamStartingPositions.size(); j++) {
				MapLocation start = this.teamStartingPositions.get(j);
				if(this.parentController.pm.isConnected(start, end)) {
					isPathConnected = true;
				}
			}
			if(!isPathConnected) {
				//no connected paths for all of our starting positions
				it.remove();
			}
		}

		//get pathfield and cache
		for(int i = 0; i < Math.min(this.MAX_PATHFIELDS, this.clusters.size()); i++) {
			Cluster q = this.clusters.get(i);
			MapLocation end = new MapLocation(this.parentController.getPlanet(), q.clusterMaxima.x, q.clusterMaxima.y);
			this.parentController.pm.getPathFieldWithCache(end);
		}

		/*
		System.out.println("Our top choices are " + topChoices);
		System.out.println(Cluster.heuristic(this.clusters.get(0)));
		System.out.println(Cluster.heuristic(this.clusters.get(this.clusters.size()-1)));*/
		/*
		for(int i = 0; i < this.clusters.size(); i++) {
			System.out.print("Cluster: " + this.clusters.get(i).clusterMaxima + " ");
			System.out.print("Heuristic value: " + Cluster.heuristic(this.clusters.get(i)));
			System.out.println();
		}
		*/
	}

	public static class Comparators {
		public static Comparator<Cluster> clusterComparator = new Comparator<Cluster>() {
			public int compare(Cluster a, Cluster b) { //select the objectively better zone
				int aa = Cluster.heuristic(a);
				int bb = Cluster.heuristic(b);
				if(aa == bb)
					return 0;
				if(aa > bb)
					return -1;
				return 1;
			}
		};
	}
}