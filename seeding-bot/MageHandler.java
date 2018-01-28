import bc.*;

public class MageHandler extends UnitHandler {

    public MageHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }

    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        
    
    @Override
    public void takeTurn(Unit unit) {

    }

    public Direction getRandomDirection(MapLocation mapLocation, MapLocation targetLocation, PathMaster pm) {
        PathField pf = pm.getPathFieldWithCache(targetLocation);
        PathField.PathPoint pp = pf.getPoint(mapLocation);
        return pp.dirs[rng.nextInt(pp.numDirs)];
    }

    public void handleDeath() {}
}