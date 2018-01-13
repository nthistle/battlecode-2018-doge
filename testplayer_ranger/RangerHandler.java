import bc.*;
import java.util.Random;

/*
Ranger with sniper ability is useless unless you have units that can see the enemy's factories.
*/

public class RangerHandler extends UnitHandler {

    private Team enemy;
    private int currSniperCooldown = 0;
    private static final UnitType snipingTarget = UnitType.Factory;
    private static final int SNIPER_COOLDOWN = 200;

    public RangerHandler(GameController gc, int id, Random rng) {
        super(gc, id, rng);
        this.enemy = Utils.getOtherTeam(gc.team());
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
        //by default, if snipe is available, most likely the rangers will snipe because most likely enemies wont be in range. need to consider changing it so only small percent of rangers snipe so we dont have a bunch clustering

        if(gc.isAttackReady(this.id)) {
            
            MapLocation myLocation = unit.location().mapLocation();
            
            //check if sniper powerup is available
            boolean sniperAvailable = gc.researchInfo().getLevel(UnitType.Ranger) == 3L;

            //if there is an enemy in range, attack them
            VecUnit nearby;
            Unit nearestEnemy;
            long nearestDist;
    
            //default attack nearest enemy in range code
            nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 50, this.enemy); // immediate range
            nearestEnemy = null;
            nearestDist = Integer.MAX_VALUE;
            for(int i = 0; i < nearby.size(); i ++) {
                if(nearestEnemy == null || (myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation()) < nearestDist)) {
                    nearestEnemy = nearby.get(i);
                    nearestDist = myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation());
                }
            }
            if(nearestEnemy != null && gc.canAttack(this.id, nearestEnemy.id())) {
                gc.attack(this.id, nearestEnemy.id());
                return;
            }


            if(sniperAvailable) {
                System.out.println("Can snipe");
                //get all enemy units that will be in range of 50 by the time sniper powerup becomes available
                int maxMovement = 0;
                int initialHeat = (int) (gc.unit(this.id).movementHeat());
                int cooldown = (int) (gc.unit(this.id).movementCooldown());
                for(int i = 0; i < 5; i++) {
                    if(initialHeat < 10) {
                        maxMovement += 1;
                        initialHeat += cooldown;
                    }
                    initialHeat -= 10;
                }
                System.out.println("The max movement is: " + maxMovement);
                VecUnit allEnemyUnits = gc.units();
                //System.out.println("allEnemyUnits: " + allEnemyUnits.toString());
                nearestEnemy = null;
                nearestDist = Integer.MAX_VALUE;
                for(int i = 0; i < allEnemyUnits.size(); i ++) {
                    if(allEnemyUnits.get(i).team() == this.enemy && (nearestEnemy == null || (myLocation.distanceSquaredTo(allEnemyUnits.get(i).location().mapLocation()) < nearestDist))) {
                        nearestEnemy = allEnemyUnits.get(i);
                        nearestDist = myLocation.distanceSquaredTo(allEnemyUnits.get(i).location().mapLocation());
                    }
                }
                
                if(nearestEnemy == null) {
                    doMovement(gc.unit(this.id));
                    return;
                }
                
                System.out.println("nearestDist: " + nearestDist + " while max: " + (50 + 2 * maxMovement));
                //maxMovement is the maximum times we can move in 5 rounds, 2 is the max distance we can move in 1 round, 50 is the closest we can attack
                if(nearestDist < 50 + 2 * maxMovement) {
                    System.out.println("Attackers can come in range after sniper charge, moving towards nearest enemy.");
                    //if there will be attackers in range by the time we start sniping, just move towards those attackers
                    if(nearestEnemy != null) {
                        //System.out.println("Moving towards enemy who is "+nearestDist+" away");
                        Utils.tryMoveWiggle(this.gc, this.id, myLocation.directionTo(nearestEnemy.location().mapLocation()));
                        return;
                    }
                } else {
                    //there are no attackers who can be in range by the time we finish sniping, lets just snipe the target with lowest health
                    Unit targetEnemy = null;
                    long currentHealth = Integer.MAX_VALUE;
                    for(int i = 0; i < allEnemyUnits.size(); i++) {
                        Unit enemyUnit = allEnemyUnits.get(i);
                        //make sure its our target type and health is low and enemy is on same planet
                        if(enemyUnit.team() == this.enemy && enemyUnit.location().isOnPlanet(myLocation.getPlanet()) && enemyUnit.unitType().compareTo(snipingTarget) == 0 && enemyUnit.health() < currentHealth) {
                            currentHealth = enemyUnit.health();
                            targetEnemy = enemyUnit;
                        }
                    }
                    //if no targets exist with criteria, choose random target
                    if(targetEnemy == null) {
                        System.out.println("Did not locate any factories");
                        /*
                        do {
                            targetEnemy = allEnemyUnits.get(new Random().nextInt((int)allEnemyUnits.size()));
                        } while (!targetEnemy.location().isOnPlanet(myLocation.getPlanet()));
                        */
                        doMovement(gc.unit(this.id));

                    } else {
                        System.out.println("Found a target factory");
                    }

                    //check if we can actually snipe at this location and that our snipe abilities are good
                    if(gc.canBeginSnipe(this.id, targetEnemy.location().mapLocation()) && gc.isBeginSnipeReady(this.id)) {
                        gc.beginSnipe(this.id, targetEnemy.location().mapLocation());
                        System.out.println("sniped");
                        return;
                    }
                }
            } else {
                //if sniper thing is not available and cannot attack just do some movement
                doMovement(gc.unit(this.id));
                return;
            }
        } else {
            //if cannot attack just do some movement
            doMovement(gc.unit(this.id));
            return;
        }
    }
    
    // note that this method is not written for efficiency right now
    // TODO write for efficiency
    private void doMovement(Unit unit) {
        // first look for enemy that's too close (<18)
        
        MapLocation myLocation = unit.location().mapLocation();
        
        VecUnit nearby;
        Unit nearestEnemy;
        Unit nearestAlly;
        long nearestDist;
        
        nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 18, this.enemy); // immediate range
        nearestEnemy = null;
        nearestDist = Integer.MAX_VALUE;
        for(int i = 0; i < nearby.size(); i ++) {
            if(nearestEnemy == null || (myLocation.distanceSquaredTo(
                    nearby.get(i).location().mapLocation()) < nearestDist)) {
                nearestEnemy = nearby.get(i);
                nearestDist = myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation());
            }
        }
        if(nearestEnemy != null) {
            Utils.tryMoveWiggle(this.gc, this.id, nearestEnemy.location().mapLocation().directionTo(myLocation));
            return;
        }
        
        // no close enemies, any close allies?

        nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 8, gc.team()); // immediate range
        nearestAlly = null;
        nearestDist = Integer.MAX_VALUE;
        for(int i = 0; i < nearby.size(); i ++) {
            if(nearby.get(i).id() == this.id) continue;
            if(nearestAlly == null || (myLocation.distanceSquaredTo(
                    nearby.get(i).location().mapLocation()) < nearestDist)) {
                nearestAlly = nearby.get(i);
                nearestDist = myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation());
            }
        }
        if(nearestAlly != null) {
            Utils.tryMoveWiggle(this.gc, this.id, nearestAlly.location().mapLocation().directionTo(myLocation));
            return;
        }
        
        // no close enemies and no close allies, how about far enemies?
        
        nearby = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.visionRange(), this.enemy); // immediate range
        nearestEnemy = null;
        nearestDist = Integer.MAX_VALUE;
        for(int i = 0; i < nearby.size(); i ++) {
            if(nearestEnemy == null || (myLocation.distanceSquaredTo(
                    nearby.get(i).location().mapLocation()) < nearestDist)) {
                nearestEnemy = nearby.get(i);
                nearestDist = myLocation.distanceSquaredTo(nearby.get(i).location().mapLocation());
            }
        }
        if(nearestEnemy != null) {
            Utils.tryMoveWiggle(this.gc, this.id, myLocation.directionTo(nearestEnemy.location().mapLocation()));
            return;
        }
        
        // wow... if all this failed, just resort to semirandom movement

        for(int i = 0; i < 5; i ++) {
            Direction moveDir = Utils.getRandomDirection(Direction.values(), this.rng);
            if(gc.canMove(this.id, moveDir)) {
                gc.moveRobot(this.id, moveDir);
                break;
            }
        }
    }
}