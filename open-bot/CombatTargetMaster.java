import bc.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.awt.Point;

public class CombatTargetMaster
{
	private PlanetController pc;
	private GameController gc;
	public ArrayList<MapLocation> targets;	
	public HashSet<Point> currentTargets;
	public int numTargetsAllowed = 15;
	public int cooldown = 0;
	public int MAX_COOLDOWN = 10;

	public CombatTargetMaster(PlanetController pc, GameController gc) {
		this.pc = pc;
		this.gc = gc;
		this.targets = new ArrayList<MapLocation>();
		this.currentTargets = new HashSet<Point>();
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
			for(int i = 0; i < vu.size(); i ++ ) {
				if(vu.get(i).team() != gc.team()) {
					MapLocation possLoc = vu.get(i).location().mapLocation();
					if(!this.currentTargets.contains(new Point(possLoc.getX(), possLoc.getY()))) {
						valid.add(possLoc);
					}
				}
			}
			if(valid.size() > 0) {
				this.addTarget(valid.get(pc.rng.nextInt(valid.size())));
				cooldown = MAX_COOLDOWN;
			} else {
				cooldown = 3;
			}
		}
	}

	public ArrayList<MapLocation> getTargets() {
		return this.targets;
	}

	public void clearTarget(MapLocation toBeRemoved) {
		this.currentTargets.remove(toBeRemoved);
		pc.pm.clearPFCache(toBeRemoved);
		if(this.targets.remove(toBeRemoved)) {
			numTargetsAllowed ++;
		}
	}

	public void clearTarget(int targetIndex) {
		MapLocation toBeRemoved = this.targets.get(targetIndex);
		this.currentTargets.remove(toBeRemoved);
		pc.pm.clearPFCache(toBeRemoved);
		this.targets.remove(targetIndex);
		numTargetsAllowed ++;
	}

	public int getNumTargets() {
		return this.targets.size();
	}

	public boolean addTarget(MapLocation ml) {
		if(this.currentTargets.contains(new Point(ml.getX(), ml.getY()))) {
			return false;
		}
		this.targets.add(ml);
		pc.pm.getPathFieldWithCache(ml);
		numTargetsAllowed --;
		this.currentTargets.add(new Point(ml.getX(), ml.getY()));
		return true;
	}
}