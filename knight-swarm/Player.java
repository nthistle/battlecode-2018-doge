import bc.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;

public class Player {

    private static final int KNIGHT_SWARM_THRESH = 20;//12; // once we have 12 knights, swarm
    private static final int KNIGHT_SWARM_STOP_THRESH = 6;
    private static final int TARGET_SWITCH_THRESH = 75;
    
    public static void main(String[] args) {
    
        Random rand = new Random();
        
        GameController gc = new GameController();
        Team myTeam = gc.team();
        Team enemy = Utils.getOtherTeam(myTeam);
        
        HashMap<Integer,UnitHandler> myHandler = new HashMap<Integer,UnitHandler>();
        
        if(gc.planet() == Planet.Mars) {
            while(true) { gc.nextTurn(); } // temporary to fix some of this logic from bugging on mars
        }
        
        PlanetMap startingMap = gc.startingMap(gc.planet());
        VecUnit startingUnits = startingMap.getInitial_units();
        
        int numTargets = 0;
        for(int i = 0; i < startingUnits.size(); i ++) {
            if(startingUnits.get(i).team()==enemy) numTargets ++;
        }
        //System.out.println(startingUnits.size() + " starting units");
        //System.out.println
        
        MapLocation[] swarmTargets = new MapLocation[numTargets];
        int cc = 0;
        for(int i = 0; i < startingUnits.size(); i ++) {
            if(startingUnits.get(i).team()==enemy) {
                swarmTargets[cc++] = startingUnits.get(i).location().mapLocation();
            }
        }

        int timeOnTarget = 0;
        MapLocation currentTarget = swarmTargets[rand.nextInt(numTargets)];
        
        boolean executeSwarm = false;
        
        while (true) {
        
            System.out.println("Round #"+gc.round());
            
            VecUnit units = gc.myUnits();
            
            int numKnights = 0;
            for(int i = 0; i < units.size(); i ++) {
                if(units.get(i).unitType()==UnitType.Knight) numKnights ++;
            }
            executeSwarm = (numKnights > KNIGHT_SWARM_THRESH) || (executeSwarm && numKnights > KNIGHT_SWARM_STOP_THRESH);
            
            if(executeSwarm) { // pick new target
                if(timeOnTarget == 0 || timeOnTarget > TARGET_SWITCH_THRESH) {
                    timeOnTarget = 0;
                    currentTarget = swarmTargets[rand.nextInt(numTargets)];
                }
                timeOnTarget ++;
            } else {
                timeOnTarget = 0;
            }
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                
                if(!myHandler.containsKey(unit.id())) {
                
                    UnitHandler newHandler = null;
                    
                    switch(unit.unitType()) {
                        case Factory:
                          newHandler = new FactoryHandler(gc, unit.id(), rand);
                          break;
                        case Knight:
                          newHandler = new KnightHandler(gc, unit.id(), rand);
                          break;
                        case Ranger:
                          newHandler = new RangerHandler(gc, unit.id(), rand);
                          break;
                        case Worker:
                          newHandler = new WorkerHandler(gc, unit.id(), rand);
                          break;
                        default:
                          break;
                    }
                    
                    myHandler.put(unit.id(), newHandler);
                }
            }
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                if(unit.unitType()!=UnitType.Factory) continue;
                myHandler.get(unit.id()).takeTurn(unit);
            }
            
            for(int i = 0; i < units.size(); i ++) {
                Unit unit = units.get(i);
                if(unit.unitType()!=UnitType.Factory)
                    myHandler.get(unit.id()).takeTurn(unit);
            }
            
            // now we do swarm stuff
            
            boolean doSwarmStep = executeSwarm;
            boolean anyFactoryFailed = false;
            for(int i = 0; i < units.size(); i ++) {
                if(units.get(i).unitType()==UnitType.Factory && ((FactoryHandler)myHandler.get(units.get(i).id())).wasUnableToUnload()) {
                    anyFactoryFailed = true;
                    break;
                }
            }
            doSwarmStep = (doSwarmStep || anyFactoryFailed);
            
            if(doSwarmStep) {
                for(int i = 0; i < 5; i ++) { // try 5 times
                    for(int j = 0; j < units.size(); j ++) {
                        if(units.get(j).unitType()!=UnitType.Knight) continue;
                        ((KnightHandler)myHandler.get(units.get(j).id())).attemptSwarm(units.get(j), currentTarget);
                    }
                }
            }
            
            if(anyFactoryFailed) {
                // try and have the factories take their turns again, see if now they can unload
                for(int i = 0; i < units.size(); i ++) {
                    if(units.get(i).unitType()==UnitType.Factory)
                        myHandler.get(units.get(i).id()).takeTurn(units.get(i));
                }
            }
            
            gc.nextTurn();
        }
    }
}