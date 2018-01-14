package nl.tudelft.cs4160.trustchain_android.keyrestore.encryption;

import java.security.SecureRandom;

public abstract class Field {

    private byte[] LOG;
    private byte[] EXP;

    /**
     * Define field addition
     * @param a
     * @param b
     * @return a + b
     */
    abstract byte add(byte a, byte b);

    /**
     * Defne field subtraction
     * @param a
     * @param b
     * @return a - b
     */
    abstract byte sub(byte a, byte b);

    /**
     * Define field multiplication
     * @param a
     * @param b
     * @return a*b
     */
    abstract byte mul(byte a, byte b);

    /**
     * Convert byte to an integer
     * @param {byte} b
     * @return {int} i
     */
    abstract int byteToUnsignedInt(byte b);

    /**
     * Define field division
     * @param a
     * @param b
     * @return a / b
     */
    abstract byte div(byte a, byte b);

    /**
     * Evaluates the polynomial {@param p} for input p({@param x}) by using Horner's method.
     * @param p A bytewise polynomial
     * @param x
     * @return P(x)
     */
    public byte eval(byte[] p, byte x) {
        // Horner's method
        byte result = 0;
        for (int i = p.length - 1; i >= 0; i--) {
            result = add(mul(result, x), p[i]);
        }
        return result;
    }

    /**
     * Determines the degree of polynomial p
     * @param p
     * @return
     */
    static int degree(byte[] p) {
        for (int i = p.length - 1; i >= 1; i--) {
            if (p[i] != 0) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Generates a random polynomial p(x) of given {@param degree} such that p(0) = y
     * @param random
     * @param degree
     * @param y
     * @return Polynomial
     */
    static byte[] generate(SecureRandom random, int degree, byte y) {
        final byte[] p = new byte[degree + 1];

        // generate random polynomials until we find one of the given degree
        do {
            random.nextBytes(p);
        } while (degree(p) != degree);

        // set y intercept
        p[0] = y;

        return p;
    }

    /**
     * Interpolates f(0) based on an array of points.
     * @param points
     * @return
     */
    byte interpolate(byte[][] points) {
        // calculate f(0) of the given points using Lagrangian interpolation
        final byte x = 0;
        byte y = 0;
        for (int i = 0; i < points.length; i++) {
            final byte aX = points[i][0];
            final byte aY = points[i][1];
            byte li = 1;
            for (int j = 0; j < points.length; j++) {
                final byte bX = points[j][0];
                if (i != j) {
                    li = mul(li, div(sub(x, bX), sub(aX, bX)));
                }
            }
            y = add(y, mul(li, aY));
        }
        return y;
    }

}
