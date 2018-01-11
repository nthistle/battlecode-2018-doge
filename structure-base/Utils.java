import bc.*;
import java.util.Random;

public class Utils
{
    public static Direction getRandomDirection(Direction[] dirs, Random rng) {
        return dirs[rng.nextInt(dirs.length)];
    }
}