import bc.*;

import java.util.Random;
import java.awt.Point;
import java.util.List;
import java.util.Iterator;

public class MarsWorkerHandler extends UnitHandler {

    MapLocation currentTarget = null;
    private Bug bug;

    public MarsWorkerHandler(PlanetController parent, GameController gc, int id, Random rng) {
        super(parent, gc, id, rng);
        this.bug = new Bug(gc, id, ((MarsController) parent).map);
    }


    public void pickNewTarget(Point currentLocation) {
        System.out.println("Picking a new target");
        Point closestPoint = null;
        long closestDistance = Integer.MAX_VALUE;
        List<Point> points = ((MarsController) this.parent).karboniteLocations;
        for(int i = 0; i < points.size(); i++) {
            if(!this.parent.pm.isConnected(new MapLocation(Planet.Mars, currentLocation.x, currentLocation.y), new MapLocation(Planet.Mars, points.get(i).x, points.get(i).y))) {
                continue;
            }
            if(Cluster.distanceSquaredTo(points.get(i), currentLocation) < closestDistance) {
                closestPoint = points.get(i);
                closestDistance = Cluster.distanceSquaredTo(points.get(i), currentLocation);
            }
        }
        if(closestPoint == null)
            return;
        this.currentTarget = new MapLocation(Planet.Mars, closestPoint.x, closestPoint.y);
        System.out.println("Picked " + this.currentTarget);
    }

        
    public void takeTurn() {
        takeTurn(this.gc.unit(this.id));
    }
    
    @Override
    public void takeTurn(Unit unit) {

        int round = (int) gc.round();

        MapLocation mapLocation = unit.location().mapLocation();

        if(round >= 750) {
             for (Direction d : Utils.directions()) {
                if (gc.canReplicate(this.id, d)) {
                    gc.replicate(this.id, d);
                    System.out.println("The miner at " + new Point(mapLocation.getX(), mapLocation.getY()) + " just replicated!");
                    MapLocation itsLocation = unit.location().mapLocation().add(d);
                    if(!gc.hasUnitAtLocation(itsLocation)) continue;
                    Unit head = gc.senseUnitAtLocation(itsLocation);
                    UnitHandler newHandler = new MarsWorkerHandler(parent, gc, head.id(), rng);
                    ((MarsController)(this.parent)).myHandler.put(head.id(), newHandler);
                    newHandler.takeTurn();
                    break;
                } else {
                    //System.out.println("We can't replicate");
                }
            }
        }

        

        //TODO change if necessary
        if(this.currentTarget == null) {
            pickNewTarget(new Point(mapLocation.getX(), mapLocation.getY()));
        }

        boolean didHarvest = doHarvest(unit);

        if(this.currentTarget == null)
            return;

        Point loc = new Point(mapLocation.getX(), mapLocation.getY());
        Point currentTargetLoc = new Point(this.currentTarget.getX(), this.currentTarget.getY());

        if(mapLocation.isWithinRange(unit.visionRange(), currentTarget)) {
            int money = (int) gc.karboniteAt(currentTarget);
            if(money == 0) {
                Iterator<Point> it = ((MarsController) this.parent).karboniteLocations.iterator();
                while(it.hasNext()) {
                    if(it.next().equals(loc)) {
                        it.remove();
                        break;
                    }
                }
                pickNewTarget(loc);
            }
        }

        if(this.currentTarget != null && !loc.equals(currentTargetLoc)) {
            if(this.parent.gc.round() % 20 == 0) {
                pickNewTarget(loc);
            }
            bug.bugMove(mapLocation, this.currentTarget);
        }
    }

    public boolean doHarvest(Unit unit) {
        Direction harvestDirection = null;  
        long mostMoney = 0;
        MapLocation mapLocation = unit.location().mapLocation();
        for (Direction d : Direction.values()) {                         
            MapLocation tryLocation = mapLocation.add(d);      
            if (this.parent.gc.startingMap(this.parent.getPlanet()).onMap(tryLocation) && gc.canHarvest(this.id, d)) {    
                long money = gc.karboniteAt(tryLocation);
                if (harvestDirection == null || money > mostMoney) {
                    harvestDirection = d;              
                    mostMoney = money;
                }
            } 
        }
        if (harvestDirection != null) {       
            gc.harvest(this.id, harvestDirection);
        }
        return true;
    }

    public void handleDeath() {
        
    }
}
