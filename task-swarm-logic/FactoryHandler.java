import bc.*;
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
                        if(parent.getSwarmRequest().size() > 0) {
                            Swarm processedSwarm = parent.getSwarmRequest().peek();
                            if(processedSwarm instanceof RangerSwarm) {
                                if(justUnloaded.unitType() == UnitType.Ranger) {
                                    processedSwarm.addUnit(justUnloaded.id());
                                    if(processedSwarm.isSwarm()) {
                                        parent.getSwarm().add(processedSwarm);
                                        System.out.println("Swarm has enough robots in it");
                                    }
                                    System.out.println("Ranger unloaded for swarm");
                                }
                            }
                        }
                    }
                }
            }
            if(!ableToUnload && garrison.size() > 0) {
                Direction[] dirs = Utils.shuffleDirectionArray(Direction.values(), this.rng);
                for(int i = 0; i < dirs.length; i++) {
                    Direction dir = dirs[this.rng.nextInt(dirs.length)];
                    if(gc.startingMap(gc.planet()).onMap(unit.location().mapLocation().add(dir)) && Utils.tryMoveWiggle(gc, gc.senseUnitAtLocation(unit.location().mapLocation().add(dir)).id(), dir) != 0) {
                        System.out.println("Forcefully moved away");
                        if(gc.canUnload(this.id, dir)) {
                            gc.unload(this.id, dir);
                            Unit justUnloaded = gc.senseUnitAtLocation(unit.location().mapLocation().add(dir));
                            if(parent.getSwarmRequest().size() > 0) {
                                Swarm processedSwarm = parent.getSwarmRequest().peek();
                                if(processedSwarm instanceof RangerSwarm) {
                                    if(justUnloaded.unitType() == UnitType.Ranger) {
                                        processedSwarm.addUnit(justUnloaded.id());
                                        if(processedSwarm.isSwarm()) {
                                            parent.getSwarm().add(processedSwarm);
                                            System.out.println("Swarm has enough robots in it");
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

        if(parent.getSwarmRequest().size() > 0) {
            Swarm processedSwarm = parent.getSwarmRequest().peek();
            if(processedSwarm.getUnits().size() == processedSwarm.getGoalSize()) {
                parent.getSwarmRequest().remove();
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