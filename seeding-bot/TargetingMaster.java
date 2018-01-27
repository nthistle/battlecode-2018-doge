import bc.*;
import java.util.ArrayList;

public class TargetingMaster
{
	private GameController gc;
	public ArrayList<MapLocation> targets;
	public ArrayList<String> initial = new ArrayList<String>();

	public TargetingMaster(GameController gc) {
		this.gc = gc;
		this.targets = new ArrayList<MapLocation>();
	}

	public MapLocation getTarget(int numFrom) {
		if(numFrom >= this.targets.size()) return null;
		return this.targets.get(this.targets.size()-numFrom-1);
	}

	public MapLocation getTarget(MapLocation requestLocation) {
		MapLocation closestTarget = null;
		long closestDistance = Long.MAX_VALUE;
		for (MapLocation tryLocation : targets) {
			long distance = requestLocation.distanceSquaredTo(tryLocation);
			if (closestTarget == null || distance < closestDistance) {
				closestTarget = tryLocation;
				closestDistance = distance;
			}
		}
		return closestTarget;
	}

	public void removeTarget(MapLocation toBeRemoved) {
		targets.remove(toBeRemoved);
	}

	public void popTarget(int numFrom) {		
		this.targets.remove(this.targets.size()-numFrom-1);
	}

	public int getNumTargets() {
		return this.targets.size();
	}

	public void addTarget(MapLocation ml) {
		this.targets.add(ml);
	}
}