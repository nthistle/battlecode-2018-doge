import bc.*;
import java.util.*;

public class ManufactureRequest {
	public MapLocation target;
	public UnitType request;
	public boolean rocketBound = false;
	
	public ManufactureRequest(UnitType request, MapLocation target, boolean rocketBound) {
		this.target = target;
		this.request = request;
		this.rocketBound = rocketBound;
	}
}