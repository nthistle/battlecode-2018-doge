import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.HashSet;

public class MiningMaster_old {

	protected int[][] initialKarboniteLocations;
	protected HashMap<int[], int[][]> clusteredLocations = new HashMap<int[], int[][]>();
	protected List<Integer> initialWorkers;
	protected int minimum_k;
	protected PlanetController parentContoller;
	protected final int KARBONITE_THRESHOLD = 5;

	public MiningMaster_old(PlanetController pc, int k, List<Integer> workers) {
		this.minimum_k = k;
		this.initialWorkers = workers;
		this.parentContoller = pc;
	}

	public void generate() {
		generateIntialKarboniteLocations();
		generateClusters();	
	}

	public HashMap<int[], int[][]> getClusters() {
		return clusteredLocations;
	}

	private void generateIntialKarboniteLocations() {
		PlanetMap initialMap = this.parentContoller.gc.startingMap(this.parentContoller.getPlanet());
		this.initialKarboniteLocations = new int[(int) initialMap.getHeight()][(int) initialMap.getWidth()];
		for(int i = 0; i < this.initialKarboniteLocations.length; i++) {
			for(int j = 0; j < this.initialKarboniteLocations[0].length; j++) {
				int base =((int) initialMap.initialKarboniteAt(new MapLocation(this.parentContoller.getPlanet(), i, j)));
				if(base > 0) {
					if(this.parentContoller.rng.nextBoolean())
						this.initialKarboniteLocations[i][j] = base - 1;
					else
						this.initialKarboniteLocations[i][j] = base + 1;
				}
			}
		}
	}

	private void generateClusters() {
		List<int[]> initialMaximas = new ArrayList<int[]>();
		int[][] initialDirections = {new int[] {0, 1}, new int[] {0, -1}, new int[] {1, 0}, new int[] {1, 1}, new int[] {1, -1}, new int[] {-1, 0}, new int[] {-1, -1}, new int[] {-1, 1}};
		for(int x = 0; x < this.initialKarboniteLocations.length; x++) {
			for(int y = 0; y < this.initialKarboniteLocations[0].length; y++) {
				boolean isMaxima = true;
				for(int[] a : initialDirections) {
					if(x+a[0] < initialKarboniteLocations.length && x+a[0] >= 0 && y+a[1] < initialKarboniteLocations[0].length && y+a[1] >= 0 && initialKarboniteLocations[x][y] < initialKarboniteLocations[x+a[0]][y+a[1]])
						isMaxima = false;
				}
				if(isMaxima && initialKarboniteLocations[x][y] > 0)
					initialMaximas.add(new int[] {x, y});
			}
		}
		List<int[]> toSkip = new ArrayList<int[]>();
		for(int[] b : initialMaximas) {
			//retarded array contains
			boolean skip = false;
			//System.out.println("Current: " + Arrays.toString(b));
			for(int i = 0; i < toSkip.size(); i++) {
				//System.out.println("Compared " + Arrays.toString(b) + " with " + Arrays.toString(toSkip.get(i)));
				if(b[0] == toSkip.get(i)[0] && b[1] == toSkip.get(i)[1]) {
					//System.out.println("Compared true");
					skip = true;
					break;
				}
			}
			if(skip) {
				//System.out.println("skipped " + Arrays.toString(b));
				continue;
			}
			//end retarded array contains
			List<int[][]> locationToCheck = new ArrayList<int[][]>();
			for(int[] a : initialDirections) {
				if(b[0]+a[0] < initialKarboniteLocations.length && b[0]+a[0] >= 0 && b[1]+a[1] < initialKarboniteLocations[0].length && b[1]+a[1] >= 0 && initialKarboniteLocations[b[0]+a[0]][b[1]+a[1]] > KARBONITE_THRESHOLD) {
					//keep a reference to the direction we calulated from
					if(initialKarboniteLocations[b[0]+a[0]][b[1]+a[1]] == initialKarboniteLocations[b[0]][b[1]])
						toSkip.add(new int[] {b[0]+a[0], b[1]+a[1]});
					locationToCheck.add(new int[][] {new int[] {b[0], b[1]}, new int[] {b[0] + a[0], b[1] + a[1]}});
				}
			}
			ListIterator<int[][]> ll = locationToCheck.listIterator();
			while(ll.hasNext()) {
				int[][] curr = ll.next();
				int[][] outerDirections = getOuterDirections(new int[] {curr[1][0] - curr[0][0], curr[1][1] - curr[0][1]});
				for(int[] a : outerDirections) {
					if(curr[1][0]+a[0] < initialKarboniteLocations.length && curr[1][0]+a[0] >= 0 && curr[1][1]+a[1] < initialKarboniteLocations[0].length && curr[1][1]+a[1] >= 0 && initialKarboniteLocations[curr[1][0]+a[0]][curr[1][1]+a[1]] > KARBONITE_THRESHOLD) {
						//keep a reference to the direction we calulated from
						ll.add(new int[][] {new int[] {curr[1][0], curr[1][1]}, new int[] {curr[1][0] + a[0], curr[1][1] + a[1]}});
					}
				}
			}
			int[][] out = new int[locationToCheck.size()][2];
			for(int x = 0; x < locationToCheck.size(); x++) {
				out[x] = locationToCheck.get(x)[1];
			}
			/*
			Set<int[]> set = new HashSet<int[]>();
			for(int i = 0; i < locationToCheck.size(); i++){
				set.add(locationToCheck[i]);
			}
			Iterator<int[]> it = set.iterator();
			while(it.hasNext()) {
			  clusters.add(it.next());
			}
			*/
			clusteredLocations.put(b, out);
		}
		/*
		String fin = "";
		for(int[] d : clusteredLocations.keySet())
			fin += " " + Arrays.toString(d);
		System.out.println("Initial Maximas: " + fin.trim());
		*/
	}

	private int[][] getOuterDirections(int[] direction) {
		if(direction[0] == 0 || direction[1] == 0)
			return new int[][] {direction};
		return new int[][] {direction, new int[] {direction[0], 0}, new int[] {0, direction[1]}};
	}

	private void printMap() {
		String[][] map = new String[initialKarboniteLocations.length][initialKarboniteLocations[0].length];
		for(int i = 0; i < map.length; i++) {
			for(int j = 0; j < map[0].length; j++) {
				if(initialKarboniteLocations[i][j] == 0) {
					map[i][j] = " ";
				}
				System.out.print(" ");
			}
		}
	}
}