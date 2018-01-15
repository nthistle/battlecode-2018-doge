import bc.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

/*
 * @author Aneesh Kotnana
 */

public abstract class Swarm
{
    protected final GameController gc;
    protected List<Integer> unitIDs;
    protected int goalSize;
    protected final int MIN_SWARM_SIZE = 8;
    protected MapLocation swarmTarget; //the final target of the path
    protected Path currPath; //temporary
    protected MapLocation swarmLeader; //robots should be following the swarmLeader
    protected boolean swarmIsMoving = false;

    public Swarm(GameController gc) {
        this.gc = gc;
        this.unitIDs = new ArrayList<Integer>();
        this.goalSize = 0; //should this be in the constructor
    }

    public void setGoalSize(int a) {
        this.goalSize = a;
    }

    public int getGoalSize() { 
        return this.goalSize;
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
        return this.currPath;
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
        this.currPath = path;
    }

    public abstract void takeTurn();
    
    public abstract void moveToLeader();

    public abstract void moveToTarget(Path path);
}