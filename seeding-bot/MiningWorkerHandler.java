import bc.*;
import java.util.Random;
import java.awt.Point;

public class MiningWorkerHandler extends UnitHandler {

    protected MapLocation target;
    protected Point miniTarget;
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
        if(this.target != null && pm.getCachedPathField(target.getX(), target.getY()) != null) {
            this.path = pm.getPathFieldWithCache(target);
            System.out.println("pathfinding using a cached pathfield");
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

        if(this.target == null)
            return;        

        boolean didHarvest = doHarvest(unit);
        Cluster c = m.clusterMap[target.getX()][target.getY()];
        Cluster f = m.clusterMap[mapLocation.getX()][mapLocation.getY()];
        if(!didHarvest && f != null && f.clusterMaxima.equals(c.clusterMaxima)) {
            Cluster a = this.m.clusterMap[mapLocation.getX()][mapLocation.getY()];
            //WE WANT TO MOVE TO A RANDOM JOINT IN THE CLUSTER
            //FIXXXXXX            
            System.out.println(a.members.size());
            if (a.members.size() == 1) {
                Point last = a.members.get(0);
                int money = (int)gc.karboniteAt(new MapLocation(parent.getPlanet(), last.x, last.y));
                if (money == 0) {
                    m.initialKarboniteLocationsOriginal[last.x][last.y] = 0;
                    m.updateIndividual(last, money);
                }
            }
            if(a.members.size() == 0) {
                System.out.println("Line 83: We don't have any more members in cluster");
                Point oldCluster = this.m.clusterMap[mapLocation.getX()][mapLocation.getY()].clusterMaxima;
                this.target = null;
                boolean hello = m.assignTarget(this);
                if(!hello) {
                    ((EarthController)(this.parent)).myHandler.put(unit.id(), new WorkerHandler(this.parent, this.gc, unit.id(), this.rng));
                    System.out.println("Converted 1 miner to a worker");  
                } else {
                    MapLocation groupTarget = this.target;
                    //we now need to figure out all the other miners at this cluster and set their targets to the same thing
                    VecUnit units = this.gc.myUnits();
                    for(int i = 0; i < units.size(); i ++) {
                        Unit unit1 = units.get(i);
                        UnitHandler unitHandler = (((EarthController)(this.parent)).myHandler.get(unit1.id()));
                        if(unitHandler instanceof MiningWorkerHandler) {
                            MiningWorkerHandler minerHandler = (MiningWorkerHandler) unitHandler;
                            if(minerHandler.target != null && this.m.clusterMap[minerHandler.target.getX()][minerHandler.target.getY()] != null && this.m.clusterMap[minerHandler.target.getX()][minerHandler.target.getY()].clusterMaxima.equals(oldCluster)) {
                                MiningWorkerHandler miner = (MiningWorkerHandler) unitHandler;
                                miner.setTarget(groupTarget, this.parent.pm);
                                if(miner.target == null) {
                                    ((EarthController)(miner.parent)).myHandler.put(miner.id, new WorkerHandler(miner.parent, miner.gc, miner.id, miner.rng));
                                    System.out.println("Converted 1 miner to a worker");  
                                } else {
                                    Cluster newCluster = this.m.clusterMap[groupTarget.getX()][groupTarget.getY()];
                                    newCluster.minersAt++;
                                }
                            }
                        }
                    }
                }
            } else {
                if(this.miniTarget == null || !this.m.clusterMap[miniTarget.x][miniTarget.y].clusterMaxima.equals(this.m.clusterMap[this.target.getX()][this.target.getY()].clusterMaxima)) {
                    //we have not picked a mini-target
                    // System.out.println("We just created a miniTarget because it was null before or wasn't part of the cluster we want to go to");
                    Point random;
                    int w = 0;
                    do {
                        random = a.members.get(this.parent.rng.nextInt(a.members.size()));
                    } while((new Point(mapLocation.getX(), mapLocation.getY()).equals(random)) && w++ < 10);
                    this.miniTarget = random;
                    Utils.tryMoveRotate(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), random.x, random.y)));    
                } else if(this.miniTarget.equals(new Point(mapLocation.getX(), mapLocation.getY())) && gc.karboniteAt(mapLocation) <= 1L) {
                    //System.out.println("We just created a miniTarget because it was null before");
                    Point random;
                    int w = 0;
                    do {
                        random = a.members.get(this.parent.rng.nextInt(a.members.size()));
                    } while((new Point(mapLocation.getX(), mapLocation.getY()).equals(random)) && w++ < 10);
                    this.miniTarget = random;
                    Utils.tryMoveRotate(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), random.x, random.y)));
                } else {
                    //System.out.println("Moving to the miniTarget");
                    if(mapLocation.directionTo(new MapLocation(parent.getPlanet(), this.miniTarget.x, this.miniTarget.y)) != Direction.Center)
                        Utils.tryMoveRotate(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), this.miniTarget.x, this.miniTarget.y)));
                }
                
            }
        } else {
            if(this.path != null && this.path.isPointSet(mapLocation.getX(), mapLocation.getY())) {
                Direction dirToMoveIn = this.path.getDirectionAtPoint(mapLocation);
                Utils.tryMoveRotate(this.gc, unit.id(), dirToMoveIn);
            } else {
                if(this.path == null) {
                    Cluster a = this.m.clusterMap[mapLocation.getX()][mapLocation.getY()];
                    if(a == null) {
                        System.out.println("Moving in a random direction");
                        Direction d = Utils.getRandomDirection(Direction.values(), this.rng);
                        Utils.tryMoveRotate(this.gc, this.id, d);
                    } else {
                        
                        if(target == null || mapLocation.directionTo(new MapLocation(parent.getPlanet(), target.getX(), target.getY())) == Direction.Center) {
                            ((EarthController)(this.parent)).myHandler.put(unit.id(), new WorkerHandler(this.parent, this.gc, unit.id(), this.rng));
                            System.out.println("Converted 1 miner to a worker C");  
                        } else {
                            //System.out.println("Move in target direction");
                        //we are trying to go somewhere that was not pathfinding cached so lets just try move wiggle
                            int dd = Utils.tryMoveWiggle(this.gc, unit.id(), mapLocation.directionTo(new MapLocation(parent.getPlanet(), target.getX(), target.getY())));
                            if(dd == 0) {
                                ((EarthController)(this.parent)).myHandler.put(unit.id(), new WorkerHandler(this.parent, this.gc, unit.id(), this.rng));
                                System.out.println("Converted 1 miner to a worker D");  
                            }
                        }
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
                        Point oldCluster = this.m.clusterMap[mapLocation.getX()][mapLocation.getY()].clusterMaxima;
                        this.target = null;
                        boolean hello = m.assignTarget(this);
                        if(!hello) {
                            ((EarthController)(this.parent)).myHandler.put(unit.id(), new WorkerHandler(this.parent, this.gc, unit.id(), this.rng));
                            System.out.println("Converted 1 miner to a worker");  
                        } else {
                            MapLocation groupTarget = this.target;
                            //we now need to figure out all the other miners at this cluster and set their targets to the same thing
                            VecUnit units = this.gc.myUnits();
                            for(int i = 0; i < units.size(); i ++) {
                                Unit unit1 = units.get(i);
                                UnitHandler unitHandler = (((EarthController)(this.parent)).myHandler.get(unit1.id()));
                                if(unitHandler instanceof MiningWorkerHandler) {
                                    MiningWorkerHandler minerHandler = (MiningWorkerHandler) unitHandler;
                                    if(minerHandler.target != null && this.m.clusterMap[minerHandler.target.getX()][minerHandler.target.getY()] != null && this.m.clusterMap[minerHandler.target.getX()][minerHandler.target.getY()].clusterMaxima.equals(oldCluster)) {
                                        MiningWorkerHandler miner = (MiningWorkerHandler) unitHandler;
                                        miner.setTarget(groupTarget, this.parent.pm);
                                        if(miner.target == null) {
                                            ((EarthController)(miner.parent)).myHandler.put(miner.id, new WorkerHandler(miner.parent, miner.gc, miner.id, miner.rng));
                                            System.out.println("Converted 1 miner to a worker");  
                                        } else {
                                            Cluster newCluster = this.m.clusterMap[groupTarget.getX()][groupTarget.getY()];
                                            newCluster.minersAt++;
                                        }
                                    }
                                }
                            }
                        }
                        return false;
                    }
                }
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
                //we have just harvested at an actual cluster
                boolean results = this.m.clusterMap[justHarvestedAt.getX()][justHarvestedAt.getY()].update(new Point(justHarvestedAt.getX(), justHarvestedAt.getY()), (int) unit.workerHarvestAmount());      
                if(!results && this.target != null) {
                    //assign these workers a new target
                    if(this.m.clusterMap[justHarvestedAt.getX()][justHarvestedAt.getY()].clusterMaxima.equals(new Point(this.target.getX(), this.target.getY()))) {
                        Point oldCluster = this.m.clusterMap[justHarvestedAt.getX()][justHarvestedAt.getY()].clusterMaxima;
                        this.target = null;
                        boolean hello = m.assignTarget(this);
                        if(!hello) {
                            ((EarthController)(this.parent)).myHandler.put(unit.id(), new WorkerHandler(this.parent, this.gc, unit.id(), this.rng));
                            System.out.println("Converted 1 miner to a worker");  
                        } else {
                            MapLocation groupTarget = this.target;
                            //we now need to figure out all the other miners at this cluster and set their targets to the same thing
                            VecUnit units = this.gc.myUnits();
                            for(int i = 0; i < units.size(); i ++) {
                                Unit unit1 = units.get(i);
                                UnitHandler unitHandler = (((EarthController)(this.parent)).myHandler.get(unit1.id()));
                                if(unitHandler instanceof MiningWorkerHandler) {
                                    MiningWorkerHandler minerHandler = (MiningWorkerHandler) unitHandler;
                                    if(minerHandler.target != null && this.m.clusterMap[minerHandler.target.getX()][minerHandler.target.getY()] != null && this.m.clusterMap[minerHandler.target.getX()][minerHandler.target.getY()].clusterMaxima.equals(oldCluster)) {
                                        MiningWorkerHandler miner = (MiningWorkerHandler) unitHandler;
                                        miner.setTarget(groupTarget, this.parent.pm);
                                        if(miner.target == null) {
                                            ((EarthController)(miner.parent)).myHandler.put(miner.id, new WorkerHandler(miner.parent, miner.gc, miner.id, miner.rng));
                                            System.out.println("Converted 1 miner to a worker");  
                                        } else {
                                            Cluster newCluster = this.m.clusterMap[groupTarget.getX()][groupTarget.getY()];
                                            newCluster.minersAt++;
                                        }
                                    }
                                }
                            }
                        }
                    } 
                }
            }
            return true;
        }
        return false;
    }

    public void handleDeath() {}
}