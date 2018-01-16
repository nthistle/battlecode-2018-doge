import bc.*;
import java.util.TreeMap;
import java.util.Random;

public class FactoryHandler extends UnitHandler {

    public FactoryHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
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
        if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
            gc.produceRobot(this.id, UnitType.Ranger);
        }

        if(unit.structureIsBuilt() != 0) {
            VecUnitID garrison = unit.structureGarrison();
            //System.out.println("Garrison size: " + garrison.size());
            boolean ableToUnload = false;
            if(garrison.size() > 0) {
                Direction[] arr = Utils.shuffleDirectionArray(Direction.values(), this.rng);
                for(int i = 0; i < 8; i ++) {
                    Direction unloadDir = arr[i];
                    if(gc.canUnload(this.id, unloadDir)) {
                        ableToUnload = true;
                        gc.unload(this.id, unloadDir);
                        Unit justUnloaded = gc.senseUnitAtLocation(unit.location().mapLocation().add(unloadDir));
                        //SWARM STUFF
                        if(parent.getSwarmCreationRequest().size() > 0) {
                            Swarm processedSwarm = parent.getSwarmCreationRequest().peek();
                            if(processedSwarm instanceof RangerSwarm) {
                                if(justUnloaded.unitType() == UnitType.Ranger) {
                                    processedSwarm.addUnit(justUnloaded.id());
                                    if(processedSwarm.isSwarm()) {
                                        if(processedSwarm.swarmTarget != null) {
                                            TreeMap<Long, MapLocation> uNits = new TreeMap<Long, MapLocation>();
                                            for(int j = 0; j < processedSwarm.getUnits().size(); j++) {
                                                Unit unit1 = gc.unit(processedSwarm.getUnits().get(j));
                                                uNits.put(Utils.distanceSquaredTo(unit1.location().mapLocation(), processedSwarm.swarmTarget), unit.location().mapLocation());
                                            }
                                            processedSwarm.setSwarmLeader(uNits.get(uNits.firstKey()));
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

            //TODO figure out what to do with this because technically nobody should be in the way because as soon as shit is created it starts random walking
            if(!ableToUnload && garrison.size() > 0) {
                Direction[] dirs = Utils.shuffleDirectionArray(Direction.values(), this.rng);
                for(int i = 0; i < dirs.length; i++) {
                    Direction dir = dirs[this.rng.nextInt(dirs.length)];
                    //TODO instead of try catch, just check if there is a unit on that location before sensing a unit on that logation
                    if(gc.canUnload(this.id, dir) && gc.hasUnitAtLocation(unit.location().mapLocation().add(dir))) {
                        if(gc.startingMap(gc.planet()).onMap(unit.location().mapLocation().add(dir)) && Utils.tryMoveWiggle(gc, gc.senseUnitAtLocation(unit.location().mapLocation().add(dir)).id(), dir) != 0) {
                            //System.out.println("Forcefully moved away");
                            if(gc.canUnload(this.id, dir)) {
                                gc.unload(this.id, dir);
                                Unit justUnloaded = gc.senseUnitAtLocation(unit.location().mapLocation().add(dir));
                                if(parent.getSwarmCreationRequest().size() > 0) {
                                    Swarm processedSwarm = parent.getSwarmCreationRequest().peek();
                                    if(processedSwarm instanceof RangerSwarm) {
                                        if(justUnloaded.unitType() == UnitType.Ranger) {
                                            processedSwarm.addUnit(justUnloaded.id());
                                            if(processedSwarm.isSwarm()) {
                                                if(processedSwarm.swarmTarget != null) {
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
                                            System.out.println("Ranger unloaded for swarm");
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        if(parent.getSwarmCreationRequest().size() > 0) {
            Swarm processedSwarm = parent.getSwarmCreationRequest().peek();
            if(processedSwarm.getGoalFactory() == 0) {
                processedSwarm.setGoalFactory(this.id);
            }
            if(processedSwarm.getGoalFactory() == this.id) {
                if(processedSwarm.getUnits().size() == processedSwarm.getGoalSize()) {
                    parent.getSwarmCreationRequest().remove();
                    System.out.println("Just removed swarm request");
                } else {
                    if(processedSwarm instanceof RangerSwarm) {
                        if(gc.canProduceRobot(this.id, UnitType.Ranger)) {
                            //System.out.println("Ranger produced for swarm");
                            gc.produceRobot(this.id, UnitType.Ranger);
                        }
                    }
                }
            }
        }
    }
}