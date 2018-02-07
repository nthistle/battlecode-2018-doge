import bc.*;
import java.util.ArrayList;

public class CombatTargetMaster
{
	private PlanetController pc;
	private GameController gc;
	public ArrayList<MapLocation> targets;	
	public int numTargetsAllowed = 15;
	public int cooldown = 0;
	public int MAX_COOLDOWN = 10;

	public CombatTargetMaster(PlanetController pc, GameController gc) {
		this.pc = pc;
		this.gc = gc;
		this.targets = new ArrayList<MapLocation>();
		VecUnit vu = gc.startingMap(gc.planet()).getInitial_units();
		for(int i = 0; i < vu.size(); i ++) {
			if(vu.get(i).team() != gc.team()) {
				addTarget(vu.get(i).location().mapLocation());
			}
		}
	}

	public void updateTM() {
		if(cooldown > 0) cooldown --;
		if(numTargetsAllowed > 0 && cooldown == 0) {
			// we can add a new target
			VecUnit vu = gc.units();
			ArrayList<MapLocation> valid = new ArrayList<MapLocation>();
		}
	}

	public ArrayList<MapLocation> getTargets() {
		return this.targets;
	}

	public void removeTarget(MapLocation toBeRemoved) {
		targets.remove(toBeRemoved);
	}

	public void clearTarget(int targetIndex) {		
		pc.pm.clearPFCache(this.targets.get(targetIndex));
		this.targets.remove(targetIndex);
		numTargetsAllowed ++;
	}

	public int getNumTargets() {
		return this.targets.size();
	}

	public void addTarget(MapLocation ml) {
		this.targets.add(ml);
		pc.pm.getPathFieldWithCache(ml);
		numTargetsAllowed --;
	}
}