package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.math.MathUtils;

//TODO remove once libGDX will fix atan2 error
public class MathUtilsFix {
    /**
     * Ref. https://discord.com/channels/348229412858101762/348229413785305089/963727517062144030
     *
     * @param i any finite double or float, but more commonly a float
     * @return an output from the inverse tangent function, from {@code -HALF_PI} to {@code HALF_PI} inclusive */
    public static float atanUnchecked (double i) {
        double n = Math.abs(i);
        double c = (n - 1.0) / (n + 1.0);
        double c2 = c * c;
        double c3 = c * c2;
        double c5 = c3 * c2;
        double c7 = c5 * c2;
        double c9 = c7 * c2;
        double c11 = c9 * c2;
        return (float)(Math.signum(i) * ((Math.PI * 0.25)
                + (0.99997726 * c - 0.33262347 * c3 + 0.19354346 * c5 - 0.11643287 * c7 + 0.05265332 * c9 - 0.0117212 * c11)));
    }

    /**
     * @param y y-component of the point to find the angle towards; note the parameter order is unusual by convention
     * @param x x-component of the point to find the angle towards; note the parameter order is unusual by convention
     * @return the angle to the given point, in radians as a float; ranges from {@code -PI} to {@code PI} */
    public static float atan2 (final float y, float x) {
        float n = y / x;
        if (n != n)
            n = (y == x ? 1f : -1f); // if both y and x are infinite, n would be NaN
        else if (n - n != n - n) x = 0f; // if n is infinite, y is infinitely larger than x.
        if (x > 0)
            return atanUnchecked(n);
        else if (x < 0) {
            if (y >= 0) return atanUnchecked(n) + MathUtils.PI;
            return atanUnchecked(n) - MathUtils.PI;
        } else if (y > 0)
            return x + MathUtils.HALF_PI;
        else if (y < 0) return x - MathUtils.HALF_PI;
        return x + y; // returns 0 for 0,0 or NaN if either y or x is NaN
    }
}
