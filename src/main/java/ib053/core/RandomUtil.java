package ib053.core;

import java.util.Random;

/**
 * Utils that work with randomness
 */
public class RandomUtil {
    public static final Random RANDOM = new Random();

    public static void shuffle(int[] items) {
        int range = items.length;
        for (int i = 0; i < items.length - 1; i++) {
            int target =  i + RANDOM.nextInt(range--);
            // Swap i and range

            int tmp = items[i];
            items[i] = items[target];
            items[target] = tmp;
        }
    }

    /** @param probability number between 0 (never) and 1 (always)
     * @return true with given probability  */
    public static boolean check(float probability) {
        return probability > RANDOM.nextFloat();
    }

    /** Picks randomly first (true) or second (false) with given weights, which can be from range [0, inf) */
    public static boolean chooseFirst(float firstWeight, float secondWeight) {
        return firstWeight < (RANDOM.nextFloat() * (firstWeight + secondWeight));
    }

    public static float clamp(float value, float min, float max) {
        if (value <= min) {
            return min;
        } else if (value >= max) {
            return max;
        } else {
            return value;
        }
    }
}
