import bc.*;
import java.util.TreeMap;
import java.util.Random;
import java.util.Iterator;

public class FactoryHandler extends UnitHandler {

    public FactoryHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }
    
    public void takeTurn() {
        try {
            this.takeTurn(gc.unit(this.id));
        } catch (Exception e) {}
    }
    
    @Override
    public void takeTurn(Unit unit) {

        /*
        int shouldProduce = this.rng.nextInt(150);
        if(shouldProduce == 0) {
            if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
                gc.produceRobot(this.id, UnitType.Ranger);
            }
        } else if(shouldProduce == 1) {
            if(gc.canProduceRobot(this.id, UnitType.Knight)) {
                gc.produceRobot(this.id, UnitType.Knight);
            }
        }
        */
        /*
        if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
            gc.produceRobot(this.id, UnitType.Ranger);
        }
        */

        if(unit.structureIsBuilt() != 0) {
            VecUnitID garrison = unit.structureGarrison();
            //System.out.println("Garrison size: " + garrison.size());
            boolean ableToUnload = false;
            if(garrison.size() > 0) {
                for(int q = 0; q < garrison.size(); q++) {
                    Direction[] arr = Utils.shuffleDirectionArray(Direction.values(), this.rng);
                    for(int i = 0; i < 8; i ++) {
                        Direction unloadDir = arr[i];
                        if(gc.canUnload(this.id, unloadDir)) {
                            ableToUnload = true;
                            gc.unload(this.id, unloadDir);
                            Unit justUnloaded = gc.senseUnitAtLocation(unit.location().mapLocation().add(unloadDir));
                            //SWARM STUFF
                            if(parent.getSwarmRequest().size() > 0) {
                                Swarm processedSwarm = null;
                                for(int z = 0; z < parent.getSwarmRequest().size(); z++) {
                                    processedSwarm = parent.getSwarmRequest().get(z);
                                    if(processedSwarm.goalFactory == this.id)
                                        break;
                                    else
                                        processedSwarm = null;
                                }
                                if(processedSwarm == null) {
                                    //hello there are no swarms to create time to dip
                                    return;
                                }
                                if(processedSwarm instanceof RangerSwarm) {
                                    if(processedSwarm.goalFactory == this.id && justUnloaded.unitType() == UnitType.Ranger) {
                                        processedSwarm.addUnit(justUnloaded.id());
                                        if(processedSwarm.isSwarm()) {
                                            if(processedSwarm.swarmTarget != null && processedSwarm.swarmLeader == null) {
                                                try {
                                                    TreeMap<Long, MapLocation> uNits = new TreeMap<Long, MapLocation>();
                                                    for(int j = 0; j < processedSwarm.getUnits().size(); j++) {
                                                        Unit unit1 = gc.unit(processedSwarm.getUnits().get(j));
                                                        uNits.put(Utils.distanceSquaredTo(unit1.location().mapLocation(), processedSwarm.swarmTarget), unit.location().mapLocation());
                                                    }
                                                    processedSwarm.setSwarmLeader(uNits.get(uNits.firstKey()));
                                                } catch (RuntimeException e) { System.out.println("error123"); }
                                            } else {
                                                processedSwarm.setSwarmLeader(gc.unit(processedSwarm.getUnits().get(0)).location().mapLocation());
                                                parent.getSwarm().add(processedSwarm);
                                                System.out.println("Swarm has enough robots in it");
                                            }
                                        }
                                        System.out.println("Ranger unloaded for swarm");
                                    }
                                }
                            }
                            //END SWARM STUFF
                        }
                    }
                }
            }
            //TODO figure out what to do with this because technically nobody should be in the way because as soon as shit is created it starts random walking
            if(!ableToUnload && garrison.size() > 0) {
                System.out.println("Unable to unload a unit");
                Direction[] dirs = Utils.shuffleDirectionArray(Direction.values(), this.rng);
                for(int i = 0; i < dirs.length; i++) {
                    Direction dir = dirs[this.rng.nextInt(dirs.length)];
                    //TODO instead of try catch, just check if there is a unit on that location before sensing a unit on that logation
                    try {
                        if(gc.hasUnitAtLocation(unit.location().mapLocation().add(dir))) {
                            //System.out.println("Can unload in dir: " + dir);
                            if(gc.startingMap(gc.planet()).onMap(unit.location().mapLocation().add(dir)) && Utils.tryMoveWiggle(gc, gc.senseUnitAtLocation(unit.location().mapLocation().add(dir)).id(), dir) != 0) {
                                System.out.println("Forcefully moved away");
                                if(gc.canUnload(this.id, dir)) {
                                    gc.unload(this.id, dir);
                                    Unit justUnloaded = gc.senseUnitAtLocation(unit.location().mapLocation().add(dir));
                                    if(parent.getSwarmRequest().size() > 0) {
                                        Swarm processedSwarm = null;
                                        for(int z = 0; z < parent.getSwarmRequest().size(); z++) {
                                            processedSwarm = parent.getSwarmRequest().get(z);
                                            if(processedSwarm.goalFactory == this.id)
                                                break;
                                            else
                                                processedSwarm = null;
                                        }
                                        if(processedSwarm == null) {
                                            //hello there are no swarms to create time to dip
                                            return;
                                        }
                                        if(processedSwarm instanceof RangerSwarm) {
                                            if(processedSwarm.goalFactory == this.id && justUnloaded.unitType() == UnitType.Ranger) {
                                                processedSwarm.addUnit(justUnloaded.id());
                                                if(processedSwarm.isSwarm()) {
                                                    if(processedSwarm.swarmTarget != null && processedSwarm.swarmLeader == null) {
                                                        TreeMap<Long, MapLocation> uNits = new TreeMap<Long, MapLocation>();
                                                        for(int j = 0; j < processedSwarm.getUnits().size(); j++) {
                                                            Unit unit1 = gc.unit(processedSwarm.getUnits().get(j));
                                                            uNits.put(Utils.distanceSquaredTo(unit1.location().mapLocation(), processedSwarm.swarmTarget), unit1.location().mapLocation());
                                                        }
                                                        processedSwarm.setSwarmLeader(uNits.get(uNits.firstKey()));
                                                    } else {
                                                        processedSwarm.setSwarmLeader(gc.unit(processedSwarm.getUnits().get(0)).location().mapLocation());
                                                        parent.getSwarm().add(processedSwarm);
                                                        System.out.println("Swarm has enough robots in it");
                                                    }
                                                }
                                                //System.out.println("Ranger unloaded for swarm");
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception e){}
                }
            }
        }

        if(parent.getSwarmRequest().size() > 0) {
            Swarm processedSwarm = null;
            for(int z = 0; z < parent.getSwarmRequest().size(); z++) {
                processedSwarm = parent.getSwarmRequest().get(z);
                if(processedSwarm.goalFactory == this.id || processedSwarm.goalFactory == 0)
                    break;
                else
                    processedSwarm = null;
            }
            if(processedSwarm == null) {
                //hello there are no swarms to create time to dip
                return;
            }

            if(processedSwarm.goalFactory == 0) {
                processedSwarm.goalFactory = this.id;
                System.out.println("Factory with id: " + this.id + " has accepted");
            }
            if(processedSwarm.goalFactory == this.id) {
                if(processedSwarm.getUnits().size() == processedSwarm.getGoalSize()) {
                    Iterator<Swarm> it = parent.getSwarmRequest().iterator();
                    while(it.hasNext()) {
                        Swarm a = it.next();
                        if(a.goalFactory == this.id) {
                            it.remove();
                            break;
                        }
                    }
                    System.out.println("Just removed swarm request");
                } else {
                    if(processedSwarm instanceof RangerSwarm) {
                        if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
                            System.out.println("Ranger produced for swarm");
                            gc.produceRobot(this.id, UnitType.Ranger);
                        }
                    }
                }
            }
        }
    }
}