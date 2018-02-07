import bc.*;

import java.util.Random;
import java.awt.Point;

public class MarsWorkerHandler extends MiningWorkerHandler {

    protected MapLocation target;
    protected Point miniTarget;
    protected PathField path;
    protected MiningMaster m;

    public MarsWorkerHandler(PlanetController parent, GameController gc, int id, Random rng, MiningMaster m) {
        super(parent, gc, id, rng, m);
        this.m = m;
    }

    public boolean hasTarget() {
        return target != null;
    }

    public void reassignAllMiners(MapLocation location) {
        // System.out.println(location);
        Point oldCluster = this.m.clusterMap[location.getX()][location.getY()].clusterMaxima;
        this.target = null;
        MapLocation groupTarget = this.target;
        //we now need to figure out all the other miners at this cluster and set their targets to the same thing
        VecUnit units = this.gc.myUnits();
        for(int i = 0; i < units.size(); i ++) {
			Unit unit1 = units.get(i);
            UnitHandler unitHandler = this.parent.myHandler.get(unit1.id());
            if(unitHandler instanceof MiningWorkerHandler) {
				MarsWorkerHandler minerHandler = (MarsWorkerHandler) unitHandler;
                if(minerHandler.target != null 
					&& this.m.clusterMap[minerHandler.target.getX()][minerHandler.target.getY()] != null 
					&& this.m.clusterMap[minerHandler.target.getX()][minerHandler.target.getY()].clusterMaxima.equals(oldCluster)) {
					MarsWorkerHandler miner = (MarsWorkerHandler) unitHandler;
                    miner.setTarget(groupTarget, this.parent.pm);
                    Cluster newCluster = this.m.clusterMap[groupTarget.getX()][groupTarget.getY()];
                    newCluster.minersAt++;
                    
                    
                }
            }
        }
    }

    public void setTarget(MapLocation a, PathMaster pm) {
        this.target = a;
        if(this.target != null && pm.getCachedPathField(target.getX(), target.getY()) != null) {
            this.path = pm.getPathFieldWithCache(target);
            this.miniTarget = new Point(a.getX(), a.getY());
            // System.out.println("pathfinding using a cached pathfield");
        } else {
            this.path = null;
        }
        
        //System.out.println(this.path==null);
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
            Cluster c = m.clusterMap[this.target.getX()][this.target.getY()];
            Cluster f = m.clusterMap[mapLocation.getX()][mapLocation.getY()];
            if(f != null && f.clusterMaxima.equals(c.clusterMaxima)) {
                //only replicate once we have reached the place we want
                if(c.minersAt < m.MAX_MINERS_AT_CLUSTER) {
                    for (Direction d : Utils.directions()) {
                        if (gc.canReplicate(this.id, d)) {
                            gc.replicate(this.id, d);
                            System.out.println("The miner at " + new Point(mapLocation.getX(), mapLocation.getY()) + " just replicated!");
                            MapLocation itsLocation = unit.location().mapLocation().add(d);
                            if(!gc.hasUnitAtLocation(itsLocation)) continue;
                            c.minersAt += 1;
                            Unit head = gc.senseUnitAtLocation(itsLocation);
                            UnitHandler newHandler = new MarsWorkerHandler(parent, gc, head.id(), rng, m);
                            ((MarsWorkerHandler) newHandler).setTarget(this.target, this.parent.pm);
                            this.parent.myHandler.put(head.id(), newHandler);
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

        if(this.target == null)
            return;

        Cluster c = m.clusterMap[target.getX()][target.getY()];
        Cluster f = m.clusterMap[mapLocation.getX()][mapLocation.getY()];
        if(f != null && f.clusterMaxima.equals(c.clusterMaxima)) {
            Cluster a = this.m.clusterMap[mapLocation.getX()][mapLocation.getY()];
            if(this.miniTarget == null && a.clusterMaxima.equals(this.m.clusterMap[this.target.getX()][this.target.getY()].clusterMaxima)) {
                //we have not picked a new miniTarget
                if(a.members.size() == 0) {
                    reassignAllMiners(mapLocation);
                } else if(a.members.size() == 1) {
                    Point last = a.members.get(0);
                    int money = (int)gc.karboniteAt(new MapLocation(parent.getPlanet(), last.x, last.y));
                    if (money == 0) {
                        m.initialKarboniteLocationsOriginal[last.x][last.y] = 0;
                        m.updateIndividual(last, money);
                        reassignAllMiners(mapLocation);
                    } else {
                        this.miniTarget = last;
                        Utils.tryMoveWiggle(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), last.x, last.y)));
                    }
                } else {
                    Point current = new Point(mapLocation.getX(), mapLocation.getY());
                    Point goal = a.members.get(0);
                    long distanceAway = Integer.MAX_VALUE;
                    for(int k = 0; k < a.members.size(); k++) {
                        long dist = Cluster.distanceSquaredTo(a.members.get(k), current);
                        if(dist < distanceAway) {
                            distanceAway = dist;
                            goal = a.members.get(k);
                        }
                    }
                    this.miniTarget = goal;
                    Utils.tryMoveWiggle(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), goal.x, goal.y)));
                }
            } else if(this.miniTarget.equals(new Point(mapLocation.getX(), mapLocation.getY())) && gc.karboniteAt(mapLocation) <= 0L) {
                //we just reached the square we want and mined it, lets pick a new location
                if(a.members.size() == 0) {
                    reassignAllMiners(mapLocation);
                } else if(a.members.size() == 1) {
                    Point last = a.members.get(0);
                    this.miniTarget = last;
                    Utils.tryMoveWiggle(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), last.x, last.y)));
                } else {
                    Point current = new Point(mapLocation.getX(), mapLocation.getY());
                    Point goal = a.members.get(0);
                    long distanceAway = Integer.MAX_VALUE;
                    for(int k = 0; k < a.members.size(); k++) {
                        long dist = Cluster.distanceSquaredTo(a.members.get(k), current);
                        if(dist < distanceAway) {
                            distanceAway = dist;
                            goal = a.members.get(k);
                        }
                    }
                    this.miniTarget = goal;
                    Utils.tryMoveWiggle(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), goal.x, goal.y)));
                }
            } else {
                //we want to move to miniTarget
                Utils.tryMoveWiggle(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), miniTarget.x, miniTarget.y)));
            }
        } else {
            if(this.path != null && this.path.isPointSet(mapLocation.getX(), mapLocation.getY())) {
                Direction dirToMoveIn = this.path.getDirectionAtPoint(mapLocation);
                Utils.tryMoveRotate(this.gc, unit.id(), dirToMoveIn);
            } else {
                //THERE ARE NO PATHFINDING JAUNTS CACHED
                if(this.path == null) {
                    Cluster a = this.m.clusterMap[mapLocation.getX()][mapLocation.getY()];
                    if(a == null) {
                        System.out.println("Moving in a random direction");
                        Direction d = Utils.getRandomDirection(Direction.values(), this.rng);
                        Utils.tryMoveRotate(this.gc, this.id, d);
                    }
                }
                if(this.path != null && !this.path.isPointSet(mapLocation.getX(), mapLocation.getY()))
                    System.out.println("isPointSet returned false");
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
                //if(this.clusterMap[mapLocation.getX()][mapLocation.getY] != null && this.clusterMap[mapLocation.getX()][mapLocation.getY].clusterMaxima.equals(new Point(this.target.getX(), this.target.getY())) && this.m.initialKarboniteLocationsOriginal[tryLocation.getX()][tryLocation.getY()] != ((int) money)) {
                if(this.m.initialKarboniteLocationsOriginal[tryLocation.getX()][tryLocation.getY()] != ((int) money)) {
                    boolean hello1 = this.m.updateIndividual(new Point(tryLocation.getX(), tryLocation.getY()), (int) money);
                    if(hello1) {
                        reassignAllMiners(tryLocation);
                        return false;
                    }
                }
                if (harvestDirection == null || money > mostMoney) {
                    harvestDirection = d;              
                    mostMoney = money;
                }
            } else if(this.m.parentController.gc.startingMap(this.m.parentController.getPlanet()).onMap(tryLocation)) {
                long money = gc.karboniteAt(tryLocation);
                //if(this.clusterMap[mapLocation.getX()][mapLocation.getY] != null && this.clusterMap[mapLocation.getX()][mapLocation.getY].clusterMaxima.equals(new Point(this.target.getX(), this.target.getY())) && this.m.initialKarboniteLocationsOriginal[tryLocation.getX()][tryLocation.getY()] != ((int) money)) {
                if(this.m.initialKarboniteLocationsOriginal[tryLocation.getX()][tryLocation.getY()] != ((int) money)) {
                    boolean hello1 = this.m.updateIndividual(new Point(tryLocation.getX(), tryLocation.getY()), (int) money);
                    if(hello1) {
                        reassignAllMiners(tryLocation);
                        return false;
                    }
                }
            }
            /*
            else if(this.m.parentController.gc.startingMap(this.m.parentController.getPlanet()).onMap(tryLocation)) {
                long money = gc.karboniteAt(tryLocation);
                //if(this.clusterMap[mapLocation.getX()][mapLocation.getY] != null && this.clusterMap[mapLocation.getX()][mapLocation.getY].clusterMaxima.equals(new Point(this.target.getX(), this.target.getY())) && this.m.initialKarboniteLocationsOriginal[tryLocation.getX()][tryLocation.getY()] != ((int) money)) {
                if(this.m.initialKarboniteLocationsOriginal[tryLocation.getX()][tryLocation.getY()] != ((int) money)) {
                    boolean hello1 = this.m.updateIndividual(new Point(tryLocation.getX(), tryLocation.getY()), (int) money);
                    if(hello1) {
                        reassignAllMiners(mapLocation);
                        return false;
                    }
                }
            }*/
        }
        if (harvestDirection != null) {       
            gc.harvest(this.id, harvestDirection);
            MapLocation justHarvestedAt = unit.location().mapLocation().add(harvestDirection);
            if(this.m.clusterMap[justHarvestedAt.getX()][justHarvestedAt.getY()] != null) {
                //we have just harvested at an actual cluster
                boolean results = this.m.clusterMap[justHarvestedAt.getX()][justHarvestedAt.getY()].update(new Point(justHarvestedAt.getX(), justHarvestedAt.getY()), (int) unit.workerHarvestAmount());
                if(results && this.target != null) {
                    //assign these workers a new target
                    if(this.m.clusterMap[justHarvestedAt.getX()][justHarvestedAt.getY()].clusterMaxima.equals(new Point(this.target.getX(), this.target.getY()))) {
                        reassignAllMiners(justHarvestedAt);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public void handleDeath() {
        if(this.target==null) {
            System.out.println("Worker could not properly handle death!");
            return;
        }
        Cluster c = this.m.clusterMap[this.target.getX()][this.target.getY()];
        c.minersAt --;
    }
}
