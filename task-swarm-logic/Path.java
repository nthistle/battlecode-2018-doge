public class Path {
	MapLocation start;
	MapLocation end;
	
	public Path(MapLocation a, MapLocation b) {
		this.start = a;
		this.end = b;
	}

	public MapLocation getStart() {
		return this.start;
	}

	public MapLocation getEnd() {
		return this.end;
	}
}