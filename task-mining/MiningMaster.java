import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
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
                teamStartingPositions.add(original.get(i).location().mapLocation());
            }
        }
	}

	private void generateIntialKarboniteLocations() {
		PlanetMap initialMap = this.parentController.gc.startingMap(this.parentController.getPlanet());
		this.initialKarboniteLocations = new int[(int) initialMap.getHeight()][(int) initialMap.getWidth()];
		this.initialKarboniteLocationsOriginal = new int[(int) initialMap.getHeight()][(int) initialMap.getWidth()];
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

	//given the id of preferrably a worker xD, it will reassign that a minerworkerhandler
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

		if(a.hasTarget()) {
			//System.out.println("This MiningWorkerHandler has a target");
			return true;
		}
		List<Cluster> needToBeFilled = new ArrayList<Cluster>();
		Cluster goal = null;
		long minDist = Integer.MAX_VALUE;

		//first we need the clusters that are accessible by this miner
		for(int i = 0; i < this.clusters.size(); i++) {
			if(this.parentController.pm.isConnected(current, new MapLocation(this.parentController.getPlanet(), this.clusters.get(i).clusterMaxima.x, this.clusters.get(i).clusterMaxima.y)))
				needToBeFilled.add(this.clusters.get(i));
			if(needToBeFilled.size() == this.MAX_CLUSTERS_VISIT)
				break;
		}

		List<Cluster> needToBeFilled1 = new ArrayList<Cluster>();
		//next, if one of the clusters we want has 0 miners at it, lets try go there
		for(int i = 0; i < needToBeFilled.size(); i++) {
			if(needToBeFilled.get(i).minersAt == 0) {
				needToBeFilled1.add(needToBeFilled.get(i));
			}
		}
		if(needToBeFilled1.size() == 0) {
			for(int i = 0; i < needToBeFilled.size(); i++) {
				if(needToBeFilled.get(i).minersAt < this.MAX_MINERS_AT_CLUSTER) {
					needToBeFilled1.add(needToBeFilled.get(i));
				}
			}
		}
		if(needToBeFilled1.size() == 0)
			return false;

		/*
		//finally select the one that is the closest to the starting location of the miner
		TreeMap<Long, Cluster> locs = new TreeMap<Long, Cluster>();
		for(int i = 0; i < needToBeFilled1.size(); i++) {
			locs.put(Cluster.distanceSquaredTo(needToBeFilled1.get(i).clusterMaxima, new Point(current.getX(), current.getY())), needToBeFilled1.get(i));
		}
		if(locs.keySet().size() > 0) {
			a.setTarget(new MapLocation(this.parentController.getPlanet(), locs.get(locs.firstKey()).clusterMaxima.x, locs.get(locs.firstKey()).clusterMaxima.y), this.parentController.pm);
			this.clusterMap[locs.get(locs.firstKey()).clusterMaxima.x][locs.get(locs.firstKey()).clusterMaxima.y].minersAt += 1;
			return true;
		} else {
			return false;
		}
		*/
		a.setTarget(new MapLocation(this.parentController.getPlanet(), needToBeFilled1.get(0).clusterMaxima.x, needToBeFilled1.get(0).clusterMaxima.y), this.parentController.pm);
		this.clusterMap[needToBeFilled1.get(0).clusterMaxima.x][needToBeFilled1.get(0).clusterMaxima.y].minersAt += 1;
		return true;
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
		printKarboniteMapWithClusters();

		Collections.sort(this.clusters, Comparators.clusterComparator);

		String topChoices = "";
		for(int i = 0; i < 3; i++) {
			topChoices += this.clusters.get(i).clusterMaxima + " ";
		}
		/*
		System.out.println("Our top choices are " + topChoices);
		System.out.println(Cluster.heuristic(this.clusters.get(0)));
		System.out.println(Cluster.heuristic(this.clusters.get(this.clusters.size()-1)));*/
		for(int i = 0; i < this.clusters.size(); i++) {
			System.out.print("Cluster: " + this.clusters.get(i).clusterMaxima + " ");
			System.out.print("Heuristic value: " + Cluster.heuristic(this.clusters.get(i)));
			System.out.println();
		}
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