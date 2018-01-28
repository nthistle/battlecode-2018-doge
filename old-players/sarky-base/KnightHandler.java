import bc.*;

import java.util.Random;

public class KnightHandler extends UnitHandler {

    public KnightHandler(GameController gc, int id, Random rng) {
        super(gc, id, rng);
    }
    
    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
    	//attack if can
		VecUnit adjacentEnemies = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 1, Utils.getOtherTeam(gc.team()));
    	if(adjacentEnemies.size() > 0 && gc.isAttackReady(this.id)) {
        	Unit victim = null;
        	long lowestHealth = Long.MAX_VALUE;
    		for(int i = 0; i < adjacentEnemies.size(); i++) {
    			if(victim == null 
    					|| adjacentEnemies.get(i).health() < lowestHealth) {
    				victim = adjacentEnemies.get(i);
    				lowestHealth = victim.health();
    			}
    		}
    		if(victim != null) {
    			gc.attack(this.id, victim.id());
    		}
    	}
    	
    	//move if enemy spotted
    	VecUnit enemiesInSight = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 50, Utils.getOtherTeam(gc.team()));
    	Unit victim = null;
    	long shortestDist = Long.MAX_VALUE;
    	long healthOfShortestDist = Long.MAX_VALUE;
    	for(int i = 0; i < enemiesInSight.size(); i++) {
    		if(victim == null
    				|| unit.location().mapLocation().distanceSquaredTo(enemiesInSight.get(i).location().mapLocation()) < shortestDist
    				|| unit.location().mapLocation().distanceSquaredTo(enemiesInSight.get(i).location().mapLocation()) == shortestDist && enemiesInSight.get(i).health() < healthOfShortestDist
    				) {
    			victim = enemiesInSight.get(i);
    			shortestDist = unit.location().mapLocation().distanceSquaredTo(enemiesInSight.get(i).location().mapLocation());
    			healthOfShortestDist = enemiesInSight.get(i).health();
    		}
    	}
    	if(victim != null) {
    		Utils.tryMoveWiggle(this.gc, this.id, unit.location().mapLocation().directionTo(victim.location().mapLocation()));
    	}
    }
}