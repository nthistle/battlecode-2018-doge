import bc.*;
import java.util.Random;
import java.awt.Point;

public class MiningWorkerHandler extends UnitHandler {

    protected MapLocation target;
    protected PathField path;
    protected MiningMaster m;

    public MiningWorkerHandler(PlanetController parent, GameController gc, int id, Random rng, MiningMaster m) {
        super(parent, gc, id, rng);
        this.m = m;
    }

    public boolean hasTarget() {
        return target != null;
    }

    public void setTarget(MapLocation a, PathMaster pm) {
        this.target = a;
        this.path = pm.generatePathField(target);
        System.out.println("Just set the path to something that is supposedly not null");
        System.out.println(this.path==null);
    }
    
    public void takeTurn() {
        takeTurn(this.gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {

        
        //TODO change if necessary
        if(!hasTarget()) {
            m.assignTarget(this);
        }
        

        MapLocation mapLocation = unit.location().mapLocation();
        if(this.target != null) {
            Cluster c = m.clusterMap[target.getX()][target.getY()];
            Cluster f = m.clusterMap[mapLocation.getX()][mapLocation.getY()];
            if(f != null && f.clusterMaxima.equals(c.clusterMaxima)) {
                //only replicate once we have reached the place we want
                if(c.minersAt < m.MAX_MINERS_AT_CLUSTER) {
                    for (Direction d : Utils.directions()) {
                        if (gc.canReplicate(this.id, d)) {
                            gc.replicate(this.id, d);
                            c.minersAt += 1;
                            Unit head = gc.senseUnitAtLocation(unit.location().mapLocation().add(d));
                            UnitHandler newHandler = new MiningWorkerHandler(parent, gc, head.id(), rng, m);
                            ((MiningWorkerHandler) newHandler).setTarget(this.target, this.parent.pm);
                            ((EarthController)(this.parent)).myHandler.put(head.id(), newHandler);
                            newHandler.takeTurn();
                            break;
                        } else {
                            //System.out.println("We can't replicate");
                        }
                    }
                }
            }
        } 
        boolean didHarvest = doHarvest(unit);
        Cluster c = m.clusterMap[target.getX()][target.getY()];
        Cluster f = m.clusterMap[mapLocation.getX()][mapLocation.getY()];
        if(!didHarvest && f != null && f.clusterMaxima.equals(c.clusterMaxima)) {
            Cluster a = this.m.clusterMap[mapLocation.getX()][mapLocation.getY()];
            if(a.maxPoint != null) {
                //let's move towards the one with the max karbonite left
                //Utils.tryMoveWiggle(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), a.maxPoint.x, a.maxPoint.y)));

                //let's move towards a random point in the cluster
                Point random = a.members.get(this.parent.rng.nextInt(a.members.size()));
                Utils.tryMoveWiggle(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), random.x, random.y)));
            }
        } else {
            if(this.path != null && this.path.isPointSet(mapLocation.getX(), mapLocation.getY())) {
                Direction dirToMoveIn = this.path.getDirectionAtPoint(mapLocation);
                if(dirToMoveIn != Direction.Center)
                    Utils.tryMoveWiggle(this.gc, unit.id(), dirToMoveIn);
            } else {
                if(this.path == null)
                    System.out.println("Need a new path/target");
                if(!this.path.isPointSet(mapLocation.getX(), mapLocation.getY()))
                    System.out.println("isPointSet returned false");
                this.target = null;
                this.path = null;
            }
        }
    }

    public boolean doHarvest(Unit unit) {
        Direction harvestDirection = null;            
        long mostMoney = 0;
        MapLocation mapLocation = unit.location().mapLocation();
        for (Direction d : Direction.values()) {                         
            MapLocation tryLocation = mapLocation.add(d);                
            if (this.m.parentController.gc.startingMap(this.m.parentController.getPlanet()).onMap(tryLocation) && gc.canHarvest(this.id, d)) {   
                long money = gc.karboniteAt(tryLocation);                                         
                if (harvestDirection == null || money > mostMoney) {
                    harvestDirection = d;                        
                    mostMoney = money;
                }                    
            }            
        }
        if (harvestDirection != null) {       
            gc.harvest(this.id, harvestDirection);
            MapLocation justHarvestedAt = unit.location().mapLocation().add(harvestDirection);
            if(this.m.clusterMap[justHarvestedAt.getX()][justHarvestedAt.getY()] != null) {
                this.m.clusterMap[justHarvestedAt.getX()][justHarvestedAt.getY()].update(new Point(justHarvestedAt.getX(), justHarvestedAt.getY()), (int) unit.workerHarvestAmount());      
            }
            return true;
        }
        return false;
    }
}