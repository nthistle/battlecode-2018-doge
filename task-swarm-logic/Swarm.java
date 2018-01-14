import bc.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public abstract class Swarm
{
    protected final GameController gc;
    protected List<Integer> unitIDs;
    protected final int MIN_SWARM_SIZE = 8;
    protected MapLocation swarmTarget; //the final target of the path
    protected Path currPath; //temporary
    protected MapLocation swarmLeader; //robots should be following the swarmLeader
    protected boolean swarmIsMoving = false;

    public Swarm() {
        this.unitIDs = new ArrayList<Integer>();
    }
    
    public void addUnit(int unitID) {
        this.unitIDs.add(unitID);
    }

    public void removeUnit(int unitID) {
        this.unitIDs.remove(unitID);
    }

    public List<Integer> getUnits() {
        return this.unitIDs;
    }

    public Path getPath() {
        return this.path;
    }

    public boolean isSwarm() {
        return this.unitIDs.size() >= this.MIN_SWARM_SIZE;
    }

    public boolean isTogether() {
        for(int i = 0; i < unitIDs.size(); i++) {
            if(!Utils.canMoveWiggle(this.gc, unitIDs.get(i), gc.unit(unitIDs.get(i)).location().mapLocation().directionTo(swarmLeader))) {
                return false;
            }
        }
        return true;
    }

    public void setSwarmLeader(MapLocation target) {
        this.swarmLeader = target;
    }

    public void setSwarmTarget(MapLocation target) {
        this.swarmTarget = target;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public abstract void doTurn();
    
    public abstract void moveToLeader();

    public abstract void moveToTarget(Path path);
}