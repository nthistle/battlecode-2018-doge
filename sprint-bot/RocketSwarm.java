import bc.*;
import java.util.*;

public class RocketSwarm extends Swarm {
	public int swarmMovementHeat = 10;
	public final int SWARM_MOVEMENT_COOLDOWN = 20;

	public RocketSwarm(GameController gc)  {
		super(gc);
	}

	public void takeTurn() {
		removeAllDeadMembers();
		this.moveToTarget();
		if(this.swarmIsMoving) {
			if(this.swarmTarget != null) {
				if(Utils.compareMapLocation(this.swarmLeader, this.swarmTarget)) {
					this.swarmIsMoving = false;
				} else {
					swarmMovementHeat -= 10;
					if(swarmMovementHeat < 10) {
						swarmMovementHeat += SWARM_MOVEMENT_COOLDOWN;
						if(this.currPath.isPointSet(this.swarmLeader.getX(), this.swarmLeader.getY())) {
							Direction dirToMoveIn = this.currPath.getDirectionAtPoint(this.swarmLeader.getX(), this.swarmLeader.getY());
							//System.out.println("We want to move in direction: " + dirToMoveIn);
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
					try {
						while(!(gc.isOccupiable(this.swarmLeader.add(d)) == 0))
							d = Utils.getRandomDirection(Direction.values(), new Random());
						System.out.println("Random walk in direction: " + d);
						this.setSwarmLeader(this.swarmLeader.add(d));
					} catch (RuntimeException e) {}
				}
				moveToLeader();
			}
		} else {
			moveToLeader();
		}
	}

	public void moveToLeader() {
		for(int j = 0; j < 5; j++) {
			for(int i = 0; i < this.unitIDs.size(); i++) {
				Unit unit = gc.unit(this.unitIDs.get(i));
				MapLocation myLocation = unit.location().mapLocation();
				Utils.tryMoveWiggle(this.gc, (this.unitIDs.get(i)), myLocation.directionTo(this.swarmLeader));
			}
		}
	}

	public void removeAllDeadMembers() {
		Iterator<Integer> it = this.unitIDs.iterator();
		while(it.hasNext()) {
			int id = it.next();
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
}