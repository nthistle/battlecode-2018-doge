import bc.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Iterator;

public class RangerSwarm extends Swarm {

	public int swarmMovementHeat = 10; 
	public final int SWARM_MOVEMENT_COOLDOWN = 20;

	public RangerSwarm(GameController gc) {
		super(gc);
	}

	public void takeTurn() {
		removeAllDeadMembers();
		this.moveToTarget();
		if(this.swarmIsMoving) {
			if(this.swarmTarget != null) {
				if(Utils.compareMapLocation(this.swarmLeader, this.swarmTarget)) {
					this.swarmIsMoving = false;
					swarmAttack(this.swarmTarget);
					this.swarmTarget = null;
				} else {
					swarmMovementHeat -= 10;
					if(swarmMovementHeat < 10) {
						swarmMovementHeat += SWARM_MOVEMENT_COOLDOWN;
						if(this.currPath.isPointSet(this.swarmLeader.getX(), this.swarmLeader.getY())) {
							Direction dirToMoveIn = this.currPath.getDirectionAtPoint(this.swarmLeader.getX(), this.swarmLeader.getY());
							System.out.println("We want to move in direction: " + dirToMoveIn);
							this.setSwarmLeader(this.swarmLeader.add(dirToMoveIn));
						} else {
							this.swarmTarget = null;
						}
					}
					moveToLeader();
				}
			} else {
				swarmMovementHeat -= 10;
				if(swarmMovementHeat < 10) {
					swarmMovementHeat += SWARM_MOVEMENT_COOLDOWN;
					Direction d = Utils.getRandomDirection(Direction.values(), new Random());
					while(!(gc.isOccupiable(this.swarmLeader.add(d)) == 0))
						d = Utils.getRandomDirection(Direction.values(), new Random());
					System.out.println("Random walk in direction: " + d);
					this.setSwarmLeader(this.swarmLeader.add(d));
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
		MapLocation attackingLocation = null;
		TreeMap<Long, MapLocation> enemies = new TreeMap<Long, MapLocation>();
		for(int i = 0; i < this.unitIDs.size(); i++) {
			Unit unit = gc.unit(this.unitIDs.get(i));
			MapLocation myLocation = unit.location().mapLocation();
			VecUnit nearby = gc.senseNearbyUnitsByTeam(myLocation, unit.attackRange(), Utils.getOtherTeam(gc.team()));
			if(nearby.size() > 0) {
				needToAttack = true;
				for(int x = 0; x < nearby.size(); x++) {
					//for rangers we want the lowest health, not nearest enemy
					//enemies.put(myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation()), nearby.get(i).location().mapLocation());
					enemies.put(nearby.get(x).health(), nearby.get(x).location().mapLocation());
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

	public void removeAllDeadMembers() {
		Iterator<Integer> it = this.unitIDs.iterator();
		while(it.hasNext()) {
			int id = it.next();
			/*
			//TODO fix this shit
			boolean inUnits = false;
			VecUnit allUnits = this.gc.myUnits();
			for(int i = 0; i < allUnits.size(); i++) {
				if(id == allUnits.get(i).id()) {
					inUnits = true;
					break;
				}	
			}
			if(!inUnits)
				it.remove();
			*/
			try {
				gc.unit(id);
			} catch(RuntimeException e) {
				it.remove();
			}
		}
	}

	public void moveToTarget() {
		if(this.isSwarm() && this.isTogether()) {
			this.swarmIsMoving = true;
			//System.out.println("swarm is moving now");
		} else if(!this.isTogether()){
			//System.out.println("swarm is not yet together");
		} else if(this.isTogether()) {
			//System.out.println("swarm is now together");
		}
	}

	public boolean moveIndividual(Integer id, MapLocation location) {
		boolean sawEnemy = false;
		MapLocation myLocation = gc.unit(id).location().mapLocation();
		Unit myUnit = gc.unit(id);
		if(myLocation.distanceSquaredTo(location) <= myUnit.visionRange()) {
			//the location we want to attack is within the unit's vision, now lets check if there is actually an enemy there
			boolean enemyVisibleLocation = true;
			Unit sensedUnit = null;
			try {
				sensedUnit = gc.senseUnitAtLocation(location);
			} catch (RuntimeException e) {
				enemyVisibleLocation = false;
				sawEnemy = false;
			}
			if(!enemyVisibleLocation) {
				//looks like the enemy doesn't exist, let's find the enemy closest to the specified location that is visible by the unit
				TreeMap<Long, Integer> enemies = new TreeMap<Long, Integer>();
				VecUnit nearbyVisible = gc.senseNearbyUnitsByTeam(myLocation, myUnit.visionRange(), Utils.getOtherTeam(gc.team()));
				if(nearbyVisible.size() > 0) {
					for(int x = 0; x < nearbyVisible.size(); x++) {
						enemies.put(Utils.distanceSquaredTo(nearbyVisible.get(x).location().mapLocation(), myLocation) , nearbyVisible.get(x).id());
					}
				}
				if(enemies.keySet().size() > 0) {
					//lets pick the enemy that's closest to our original target to attack
					sensedUnit = gc.unit(enemies.get(enemies.firstKey()));
					location = sensedUnit.location().mapLocation();
					sawEnemy = true;
				} else {
					//unfortunately no enemies were in range, let's move closer to our original target
					Utils.tryMoveRotate(this.gc, myUnit, myUnit.location().mapLocation().directionTo(location));
					return false;
				}
			}
			//now we have a unit and a location we want to target, lets check if we are within attacking range
			if(myLocation.distanceSquaredTo(location) <= myUnit.attackRange()) {
				//we are in luck, lets attack the enemy
				if(gc.canAttack(id, sensedUnit.id()) && myUnit.attackHeat() < 10) {
            		gc.attack(id, sensedUnit.id());
            		sawEnemy = true;
            	}
			} else {
				//looks like we need to move closer
				Utils.tryMoveRotate(this.gc, myUnit, myUnit.location().mapLocation().directionTo(location));
			}
		} else {
			//looks like we need to move closer
			Utils.tryMoveRotate(this.gc, myUnit, myUnit.location().mapLocation().directionTo(location));
			return false;
		}
		return sawEnemy;
	}

	public void swarmAttack(MapLocation location) {
		List<Boolean> results = new ArrayList<>();
		//make sure we try multiple times
		for(int x = 0; x < 3; x++) {
			for(int i = 0; i < this.unitIDs.size(); i++) {
				int id = this.unitIDs.get(i);
				results.add(moveIndividual(id, location));
			}
		}
		/*
		for(int i = 0; i < results.size(); i++) {
			if(results.get(i))
				return;
		}
		this.swarmTarget = null;
		moveToLeader();
		*/
	}
}