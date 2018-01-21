import bc.*;
import java.util.LinkedList;
import java.util.Queue;

public class PathMaster
{
	protected PlanetMap basemap;
	protected PathField[][] pathFieldCache;
	
	public PathMaster(PlanetMap basemap) {
		this.basemap = basemap;
		this.pathFieldCache = new PathField[(int)basemap.getWidth()][(int)basemap.getHeight()];
	}

	public PathField getPathField(MapLocation target) {
		int x = target.getX();
		int y = target.getY();
		if(this.pathFieldCache[x][y]==null) {
			this.pathFieldCache[x][y] = generatePathField(target);
		}
		return this.pathFieldCache[x][y];
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
			if(pf.isPointSet(cur.x, cur.y)) {
				if(pf.getPoint(cur.x, cur.y).dist == cur.dist)
					// if the point is set and the distances are the same, add the extra direction as a possibility
					pf.addDirection(cur.x, cur.y, cur.dir);
				continue;
			} else {
				// if it's not set yet, we're going to set it and then add neighboring directions 
				pf.setPoint(cur.x, cur.y, cur.dir, cur.dist);
			}
			for(Direction dir : Utils.directions()) {
				BFSLocation possLoc = cur.add(dir);
				if(pf.isPointValid(possLoc.x, possLoc.y) && checkPassable(cur.x, cur.y)) {
					// we'll add the point to the queue if it hasn't been set yet, or if it's going to end up as another
					// with the same distance
					if(!pf.isPointSet(possLoc.x, possLoc.y) || pf.getPoint(possLoc.x, possLoc.y).dist == possLoc.dist)
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