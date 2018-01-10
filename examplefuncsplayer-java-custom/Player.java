// import the API.
// See xxx for the javadocs.
import bc.*;
import java.util.Random;

public class Player {

    public static Direction getRandDirection(Direction[] dirs, Random rand) {
        return dirs[rand.nextInt(dirs.length)];
    }
    
    public static void main(String[] args) {
    
        Random r = new Random(); //(1337);
        
        // MapLocation is a data structure you'll use a lot.
        //MapLocation mloc = new MapLocation(Planet.Earth, 10, 20);
        //System.out.println("loc: "+mloc+", one step to the Northwest: "+loc.add(Direction.Northwest));
        //System.out.println("loc x: "+mloc.getX());

        // One slightly weird thing: some methods are currently static methods on a static class called bc.
        // This will eventually be fixed :/
        System.out.println("Opposite of " + Direction.North + ": " + bc.bcDirectionOpposite(Direction.North));

        // Connect to the manager, starting the game
        GameController gc = new GameController();
        
        Team myTeam = gc.team();

        // Direction is a normal java enum.
        Direction[] directions = Direction.values();

        while (true) {
            System.out.println("Current round: "+gc.round());
            // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
            VecUnit units = gc.myUnits();
            for (int i = 0; i < units.size(); i++) {
                Unit unit = units.get(i);
                
                Direction d;
                
                if(unit.unitType()==UnitType.Factory) {
                    VecUnitID garrison = unit.structureGarrison();
                    if(garrison.size() > 0) {
                        d = getRandDirection(directions, r);
                        try {
                            if(gc.canUnload(unit.id(), d)) {
                                gc.unload(unit.id(), d);
                                System.out.println("Unloaded a Knight!");
                            } else if(gc.canProduceRobot(unit.id(), UnitType.Knight)) {
                                gc.produceRobot(unit.id(), UnitType.Knight);
                                System.out.println("Produced a Knight!");
                            }
                        } catch(Exception e) {e.printStackTrace();}
                    }
                }
                
                // first, let's look for nearby blueprints to work on
                Location loc = unit.location();
                if(loc.isOnMap()) {
                    VecUnit nearby = gc.senseNearbyUnits(loc.mapLocation(), 2L);
                    for(int j = 0; j < nearby.size(); j ++) {
                        Unit nearbyUnit = nearby.get(j);
                        if(unit.unitType()==UnitType.Worker && gc.canBuild(unit.id(), nearbyUnit.id())) {
                            System.out.println("Built a factory!");
                            gc.build(unit.id(), nearbyUnit.id());
                            continue;
                        }
                        if(nearbyUnit.team() != myTeam && gc.isAttackReady(unit.id()) && gc.canAttack(unit.id(), nearbyUnit.id())) {
                            System.out.println("Attacked a thing!");
                            gc.attack(unit.id(), nearbyUnit.id());
                        }
                    }
                }
                
                // okay, there weren't any dudes around
                // pick a random direction
                d = getRandDirection(directions, r);
                
                if(gc.karbonite() > 50 && gc.canBlueprint(unit.id(), UnitType.Factory, d)) {
                    System.out.println("Blueprinting factory!");
                    gc.blueprint(unit.id(), UnitType.Factory, d);
                } else if(gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), d)) {
                    //System.out.println("Moved!");
                    gc.moveRobot(unit.id(), d);
                }
            }
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
}