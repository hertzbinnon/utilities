package com.jiminger.nr;

import com.jiminger.nr.Minimizer.Func;

/**
 * <p>
 * This class will do a linear regression by minimizing the squared error
 * between the points provided to the constructor and the line specified
 * by y = m[0]x + m[1]
 * </p>
 * 
 * <p>
 * NOTE: The error term is calculated using the vertical distance. That is
 * it minimizes the error in the 'y' term only. This is more robust for determining
 * the slope/intercept form of a line because the starting point cannot be rotated
 * through the Y axis. In other words, if the slope of the points is approximately
 * -3.0 and the slope of the starting iteration is 3.0, then RAISING the slope
 * lowers the perpendicular distance. This moves the iterations in the wrong
 * direction.
 * </p>
 */
public class LinearRegressionWithKnownSlope implements Func {
   private final double[] y;
   private final double[] x;
   private double slope = 0.0;

   public LinearRegressionWithKnownSlope(final double slope, final double[] x, final double[] y) {
      this.x = x;
      this.y = y;
      this.slope = slope;
   }

   @Override
   public double func(final double[] m) {
      double error2 = 0.0;
      for(int i = 0; i < x.length; i++) {
         final double ycur = (slope * x[i] + m[0]);
         final double ecur = ycur - y[i];
         error2 += (ecur * ecur);
      }

      return error2;
   }

}
