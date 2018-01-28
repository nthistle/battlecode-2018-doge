import bc.*;
import java.util.ArrayList;

public class TargetingMaster
{
	private GameController gc;
	private ArrayList<MapLocation> targets;

	public TargetingMaster(GameController gc) {
		this.gc = gc;
		this.targets = new ArrayList<MapLocation>();
	}

	public MapLocation getTarget(int numFrom) {
		if(numFrom >= this.targets.size()) return null;
		return this.targets.get(this.targets.size()-numFrom-1);
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