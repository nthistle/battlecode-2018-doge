import bc.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;

public class RangerSwarm extends Swarm {

	public int swarmMovementHeat = 10; 
	public final int SWARM_MOVEMENT_COOLDOWN = 20;

	public RangerSwarm(GameController gc) {
		super(gc);
	}

	public void takeTurn() {
		this.moveToTarget();
		if(this.swarmIsMoving) {
			if(this.swarmTarget != null) {
				if(Utils.compareMapLocation(this.swarmLeader, this.swarmTarget)) {
					this.swarmIsMoving = false;
					swarmAttack(this.swarmTarget);
				} else {
					swarmMovementHeat -= 10;
					if(swarmMovementHeat < 10) {
						swarmMovementHeat += SWARM_MOVEMENT_COOLDOWN;
						Direction dirToMoveIn = this.currPath.getDirectionAtPoint(this.swarmLeader.getX(), this.swarmLeader.getY());
						System.out.println("At " + this.swarmLeader.getX() + ", " + this.swarmLeader.getY() + " we want to move " + dirToMoveIn);
						this.setSwarmLeader(this.swarmLeader.add(dirToMoveIn));
					}
					moveToLeader();
				}
			} else {
				moveToLeader();
			}
		} else {
			moveToLeader();
		}
	}

	public void moveToLeader() {
		//checking if any of the swarm members can see an attacker. If attacker is found, it prioritizes attackers based on closest attacker
		boolean needToAttack = false;
		MapLocation attackingLocation = null;
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
		if(enemies.keySet().size() > 0) {
			attackingLocation = enemies.get(enemies.firstKey());
		}
		if(needToAttack) {
			swarmAttack(attackingLocation);
		} else {
			for(int j = 0; j < 5; j++) {
				for(int i = 0; i < this.unitIDs.size(); i++) {
					Unit unit = gc.unit(this.unitIDs.get(i));
					MapLocation myLocation = unit.location().mapLocation();
					Utils.tryMoveWiggle(this.gc, (this.unitIDs.get(i)), myLocation.directionTo(this.swarmLeader));
				}
			}
		}
	}

	public void moveToTarget() {
		if(this.isSwarm() && this.isTogether()) {
			this.swarmIsMoving = true;
			System.out.println("swarm is moving now");
		} else if(!this.isTogether()){
			System.out.println("swarm is not yet together");
		} else if(this.isTogether()) {
			System.out.println("swarm is now together");
		}
	}

	public void swarmAttack(MapLocation location) {
	/*
		boolean enemySeen = false;
		for(int i = 0; i < this.unitIDs.size(); i++) {
			if
		}
	*/
	}
}