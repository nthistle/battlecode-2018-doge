import bc.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashMap;
import java.util.ArrayList;
import java.awt.Point;

public class PathMaster
{
	protected PlanetMap basemap;
	protected HashMap<Point,PathField> limitedCache;
	protected int cacheCount;
	protected int[][] labels;
	
	public PathMaster(PlanetMap basemap) {
		this.basemap = basemap;
		// this.pathFieldCache = new PathField[(int)basemap.getWidth()][(int)basemap.getHeight()];
		this.labels = this.generateLabels();
		this.limitedCache = new HashMap<Point,PathField>();
		this.cacheCount = 0;
	}
	
	public boolean isConnected(MapLocation a, MapLocation b) {
		int i1 = a.getY(), j1 = a.getX(), i2 = b.getY(), j2 = b.getX();
		return this.labels[i1][j1] > 0 && this.labels[i2][j2] > 0 && this.labels[i1][j1] == this.labels[i2][j2];
	}
	
	private int[][] generateLabels() {
		int[][] ret = new int[(int)this.basemap.getHeight()][(int)this.basemap.getWidth()];
		int zone = 0;
		for(int i = 0; i < ret.length; i++) {
			for(int j = 0; j < ret[i].length; j++) {
				if(this.basemap.isPassableTerrainAt(new MapLocation(this.basemap.getPlanet(), j, i)) == 0) {
					ret[i][j] = -1;
				}
				else if(ret[i][j] == 0) {
					zone++;
					recur(ret, i, j, zone);
				}
			}
		}
		return ret;
	}
	
	private void recur(int[][] ret, int i, int j, int tag) {
		if(i < 0
				|| i >= basemap.getHeight()
				|| j < 0
				|| j >= basemap.getWidth()
				|| ret[i][j] != 0
				|| this.basemap.isPassableTerrainAt(new MapLocation(this.basemap.getPlanet(), j, i)) == 0) 
			return;
		else {
			ret[i][j] = tag;
			recur(ret, i+1, j, tag);
			recur(ret, i+1, j+1, tag);
			recur(ret, i, j+1, tag);
			recur(ret, i-1, j+1, tag);
			recur(ret, i-1, j, tag);
			recur(ret, i-1, j-1, tag);
			recur(ret, i, j-1, tag);
			recur(ret, i+1, j-1, tag);
		}
	}

	// warning, this method WILL cache the path that you ask for
	public PathField getPathFieldWithCache(MapLocation target) {
		PathField cachedPf = getCachedPathField(target.getX(), target.getY());
		if(cachedPf == null)
			return getAndCachePathField(target);
		return cachedPf;
	}

	public void clearPFCache(MapLocation target) {
		this.limitedCache.remove(new Point(target.getX(), target.getY()));
	}

	public PathField getPathField(MapLocation target) {
		int x = target.getX();
		int y = target.getY();
		// if(this.pathFieldCache[x][y]==null) {
			// this.pathFieldCache[x][y] = generatePathField(target);
		// }
		return generatePathField(target);
		// return this.pathFieldCache[x][y];
	}

	public void cachePathField(int x, int y, PathField pf) {
		limitedCache.put(new Point(x,y), pf);
	}

	public PathField getCachedPathField(int x, int y) {
		return limitedCache.get(new Point(x,y));
	}

	public PathField getAndCachePathField(MapLocation target) {
		PathField pf = this.generatePathField(target);
		this.cachePathField(target.getX(), target.getY(), pf);
		return pf;
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