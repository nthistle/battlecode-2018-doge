import bc.*;
import java.util.LinkedList;
import java.util.Queue;

public class PathMaster
{
	protected PlanetMap basemap;
	
	public PathMaster(PlanetMap basemap) {
		this.basemap = basemap;
	}

	public PathField getPathField(MapLocation target) {
		// if it's cached, return the cached pathfield
		// if it's not, call generate and cache it
		return null; // filler
	}

	public PathField generatePathField(MapLocation target) {
		// Does BFS, assigning directions back at each location
		Queue<BFSLocation> queue = new LinkedList<BFSLocation>();
		BFSLocation cur = fromMapLocation(target);
		queue.add(cur);

		PathField pf = new PathField(this.basemap, target);
		//pf.setPoint(cur.x, cur.y, cur.dir, 0);

		while(queue.size() > 0) {
			cur = queue.poll();
			if(pf.isPointSet(cur.x, cur.y))
				continue;
			pf.setPoint(cur.x, cur.y, cur.dir, cur.dist);
			for(Direction dir : Utils.directions()) {
				BFSLocation possLoc = cur.add(dir);
				if(pf.isPointValid(possLoc.x, possLoc.y) && !pf.isPointSet(possLoc.x, possLoc.y) && checkPassable(cur.x, cur.y)) {
					queue.add(possLoc);
				}
			}
		}
		return pf;
	}

	private boolean checkPassable(int x, int y) {
		return this.basemap.isPassableTerrainAt(new MapLocation(basemap.getPlanet(), x, y)) != 0;
	}

	private BFSLocation fromMapLocation(MapLocation ml) {
		return fromMapLocation(ml, Direction.Center, 0);
	}

	private BFSLocation fromMapLocation(MapLocation ml, Direction dir, int dist) {
		return new BFSLocation(ml.getX(), ml.getY(), dir, dist);
	}

	private class BFSLocation
	{
		public final int x;
		public final int y;
		public final Direction dir;
		public final int dist;

		public BFSLocation(int x, int y, Direction dir, int dist) {
			this.x = x;
			this.y = y;
			this.dir = dir;
			this.dist = dist;
		}

		public BFSLocation add(Direction dir) {
			return fromMapLocation(new MapLocation(Planet.Earth, this.x, this.y).add(dir), Utils.reverseDirection(dir), this.dist+1);
		}
	}

}