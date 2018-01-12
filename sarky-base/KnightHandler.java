import bc.*;
import sun.misc.GC;

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
    	//simply attack if can
    	VecUnit adjacentEnemies = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 1, Utils.getOtherTeam(gc.team()));
    	if(adjacentEnemies.size() > 0) {
    		for(int i = 0; i < adjacentEnemies.size(); i++) {
    			if(gc.canAttack(this.id, adjacentEnemies.get(i).id())) {
    				gc.attack(this.id, adjacentEnemies.get(i).id());
    				break;
    			}
    		}
    	}
    }
}