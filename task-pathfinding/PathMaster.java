import bc.*;
import java.util.LinkedList;
import java.util.Queue;

public class PathMaster
{
	protected PlanetMap basemap;
	
	public PathMaster(PlanetMap basemap) {
		this.basemap = basemap;
	}

	public void generatePathField(MapLocation target) {
		// Does BFS, assigning directions back at each location
		Queue<BFSLocation> queue = new LinkedList<BFSLocation>();
		queue.add(BFSLocation.fromMapLocation(target));

		BFSLocation cur;
		while(queue.size() > 0) {
			cur = queue.poll();
			// mark on the map
		}


	}

	private class BFSLocation
	{
		public final int x;
		public final int y;
		public final int dist;

		public BFSLocation(int x, int y, int dist) {
			this.x = x;
			this.y = y;
			this.dist = dist;
		}

		public static BFSLocation fromMapLocation(MapLocation ml) {
			return fromMapLocation(ml, 0);
		}

		public static BFSLocation fromMapLocation(MapLocation ml, int dist) {
			return new BFSLocation(ml.getX(), ml.getY(), dist);
		}
	}

}