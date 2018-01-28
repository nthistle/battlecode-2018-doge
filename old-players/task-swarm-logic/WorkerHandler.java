import bc.*;
import java.util.Random;

public class WorkerHandler extends UnitHandler {

	public static int factoriesCreated = 0;
	private int curFactoryCooldown = 0;
    private static final int FACTORY_COOLDOWN = 50;

    public WorkerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }

    public void takeTurn() {
        this.takeTurn(gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {
		VecUnit nearby = gc.senseNearbyUnits(unit.location().mapLocation(), 2); // immediate range
        for(int i = 0; i < nearby.size(); i ++) {
            if(gc.canBuild(this.id, nearby.get(i).id())) {
                gc.build(this.id, nearby.get(i).id());
                return;
            }
        }
        
        // cooldown starts ticking after *done* building 
        if(curFactoryCooldown > 0) curFactoryCooldown --;
        
        // can't build anything, try moving
        if(gc.isMoveReady(this.id)) {
            for(int i = 0; i < 5; i ++) {
                Direction moveDir = Utils.getRandomDirection(Direction.values(), this.rng);
                if(gc.canMove(this.id, moveDir)) {
                    gc.moveRobot(this.id, moveDir);
                    break;
                }
            }
        }
        
        for(int i = 0; i < 5; i ++) {
            Direction bpDir = Utils.getRandomDirection(Direction.values(), this.rng);
            if(this.factoriesCreated < 2 && gc.canBlueprint(this.id, UnitType.Factory, bpDir)) {
                gc.blueprint(this.id, UnitType.Factory, bpDir);
                this.curFactoryCooldown = FACTORY_COOLDOWN;
                this.factoriesCreated++;
            }
        }
    }
}