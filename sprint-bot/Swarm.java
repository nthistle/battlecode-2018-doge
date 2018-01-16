import bc.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;
import java.util.HashMap;
import java.util.Arrays;

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
    protected PathField currPath; //temporary
    protected MapLocation swarmLeader; //robots should be following the swarmLeader
    protected boolean swarmIsMoving = false;
    protected int goalFactory;

    public Swarm(GameController gc) {
        this.gc = gc;
        this.unitIDs = new ArrayList<Integer>();
        this.goalSize = 0; //should this be in the constructor
    }

    public void setGoalSize(int a) {
        this.goalSize = a;
    }

    public void setGoalFactory(int a) {
        this.goalFactory = a;
    }

    public int getGoalFactory() {
        if(this.goalFactory == 0)
            return 0;
        return this.goalFactory;
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

    public PathField getPath() {
        return this.currPath;
    }

    public boolean isSwarm() {
        return this.unitIDs.size() >= this.MIN_SWARM_SIZE;
    }

    public void setPath(PathField path) {
        this.currPath = path;
        this.swarmTarget = path.getTargetLocation();
    }

    public boolean isTogether() {
        /*
        for(int i = 0; i < unitIDs.size(); i++) {
            if(!Utils.canMoveWiggle(this.gc, unitIDs.get(i), gc.unit(unitIDs.get(i)).location().mapLocation().directionTo(swarmLeader))) {
                return false;
            }
        }
        return true;
        */

        /*
        HashMap<Long, Integer> map = new HashMap<Long, Integer>();
        Long[] distancesFromLeader = new Long[this.unitIDs.size()];
        for(int i = 0; i < distancesFromLeader.length; i++) {
            distancesFromLeader[i] = this.swarmLeader.distanceSquaredTo(gc.unit(this.unitIDs.get(i)).location().mapLocation());
            map.put(distancesFromLeader[i], (this.unitIDs.get(i)));
        }
        long std_dev = 0L;
        long mean = 0L;
        for(int i = 0; i < distancesFromLeader.length; i++) {
            mean += distancesFromLeader[i];
        }
        if(distancesFromLeader.length == 0) {
            return false;
        }
        mean /= distancesFromLeader.length;
        for(int i = 0; i < distancesFromLeader.length; i++) {
            std_dev += Math.pow((distancesFromLeader[i] - mean), 2);    
        }
        std_dev = (long) Math.sqrt(std_dev/((long) distancesFromLeader.length));
        List<Integer> offenders = new ArrayList<>();
        for(int i = 0; i < distancesFromLeader.length; i++) {
            if(Math.abs(distancesFromLeader[i]-mean) > std_dev)
                offenders.add(map.get(distancesFromLeader[0]));
        }
        //System.out.println("Mean distance from leader: " + mean);
        //System.out.println("STD DEV: " + std_dev);
        //System.out.println(Arrays.toString(distancesFromLeader));
        return !(offenders.size() > 0);
        */
        for(int i = 0; i < this.unitIDs.size(); i++) {
            Unit unit = gc.unit(this.unitIDs.get(i));
            MapLocation myLocation = unit.location().mapLocation();
            //System.out.println(Utils.canMoveWiggle(this.gc, this.unitIDs.get(i), myLocation.directionTo(this.swarmLeader)));
            if(Utils.canMoveWiggle(this.gc, this.unitIDs.get(i), myLocation.directionTo(this.swarmLeader)) != 0)
                return false;
        }
        return true;
    }

    public void setSwarmLeader(MapLocation target) {
        this.swarmLeader = target;
    }

    public void setSwarmTarget(MapLocation target) {
        this.swarmTarget = target;
    }

    public abstract void takeTurn();
    
    public abstract void moveToLeader();

    public abstract void moveToTarget();
}