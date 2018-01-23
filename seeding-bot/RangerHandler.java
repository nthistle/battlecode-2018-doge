import bc.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

public class RangerHandler extends UnitHandler {

    public RangerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
    }

    public void takeTurn() {
        takeTurn(gc.unit(this.id));
    }        
    
    @Override
    public void takeTurn(Unit unit) {        

        Location location = unit.location();

        if (!location.isOnMap()) {            
            return;
        }

        MapLocation mapLocation = location.mapLocation();

        if (location.isOnPlanet(Planet.Mars)) {
            System.out.println("LITTINGTON_BILLINGTON");
            return;
        }

        // references to parent
        EarthController earthParent = (EarthController)parent;
        PlanetMap map = earthParent.map;
        Team enemyTeam = earthParent.enemyTeam;
        TargetingMaster tm = earthParent.tm;

        MapLocation target = getTarget(mapLocation, unit.visionRange(), enemyTeam, tm);

    }

    private MapLocation getTarget(MapLocation requestLocation, long range, Team enemyTeam, TargetingMaster tm) {
        while (true) {
            MapLocation target = tm.getTarget(requestLocation);
            if (target == null) {
                break;
            }
            if (target.isWithinRange(range, requestLocation) && gc.senseNearbyUnitsByTeam(target, range, enemyTeam).size() == 0) {
                tm.removeTarget(target);
                continue;
            }
            return target;
        }        
        return null;
    }
}