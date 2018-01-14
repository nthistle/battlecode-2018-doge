import bc.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class RangerSwarm extends Swarm {

	public int swarmMovementHeat = 10; 
	public final int SWARM_MOVEMENT_COOLDOWN = 20;

	public RangerSwarm() {
		super();
	}

	public void doTurn() {
		if(this.swarmIsMoving) {
			if(Utils.compareMapLocation(this.smarmLeader, this.swarmTarget)) {
				this.swarmIsMoving = false;
				swarmAttack(this.swarmTarget);
			} else {
				swarmMovementHeat -= 10;
				if(swarmMovementHeat < 10) {
					swarmMovementHeat += SWARM_MOVEMENT_COOLDOWN;
					//TODO update swarmLeader location
				}
				moveToLeader();
			}
		} else {
			moveToLeader();
		}
	}

	public void moveToLeader() {
		//checking if any of the swarm members can see an attacker. If attacker is found, it prioritizes attackers based on closest attacker
		boolean needToAttack = false;
		MapLocation attackingLocation;
		TreeMap<Long, MapLocation> enemies = new TreeMap<Long, MapLocation>();
		for(int i = 0; i < this.unitIDs.size(); i++) {
			Unit unit = gc.unit(this.unitIDs.get(i));
			MapLocation myLocation = unit.location().mapLocation();
			VecUnit nearby = gc.senseNearbyUnitsByTeam(myLocation, unit.attackRange(), Utils.getOtherTeam(gc.team()));
			if(nearby.size() > 0) {
				needToAttack = true;
				for(int x = 0; x < nearby.size(); x++) {
					enemies.put(myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation()), nearby.get(i).location().mapLocation());
				}
			}
		}
		attackingLocation = enemies.get(enemies.firstKey());
		if(needToAttack) {
			swarmAttack(attackingLocation);
		} else {
			for(int i = 0; i < this.unitIDs.size(); i++) {
				Unit unit = gc.unit(this.unitIDs.get(i));
				MapLocation myLocation = unit.location().mapLocation();
				Utils.tryMoveWiggle(this.gc, this.unitIDs.get(i), myLocation.location().mapLocation().directionTo(this.swarmLeader));
			}
		}
	}

	public void moveToTarget(Path path) {
		if(this.isSwarm() && this.isTogether()) {
			this.swarmIsMoving = true;
		}
		this.setPath(path)
		this.setSwarmTarget(MapLocation(Planet.EARTH, 0, 0)); //TODO make this get the swarmTarget from the path
	}

	public void swarmAttack(MapLocation location) {
		System.out.println("Whole swarm attacks this location: " + location.toString());
	}
}