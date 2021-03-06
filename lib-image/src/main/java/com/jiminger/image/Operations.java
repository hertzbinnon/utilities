package com.jiminger.image;

import java.awt.Color;
import java.awt.image.IndexColorModel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.imgproc.Imgproc;

import com.jiminger.image.CvRaster.Closer;
import com.jiminger.image.CvRaster.ImageOpContext;

public class Operations {
   public static byte EDGE = (byte)-1;
   public static final byte ROVERLAY = (byte)100;
   public static final byte GOVERLAY = (byte)101;
   public static final byte BOVERLAY = (byte)102;
   public static final byte YOVERLAY = (byte)103;
   public static final byte COVERLAY = (byte)104;
   public static final byte MOVERLAY = (byte)105;
   public static final byte OOVERLAY = (byte)106;
   public static final byte GRAYOVERLAY = (byte)107;

   private static final double[] cvrtScaleDenom = new double[6];

   public static final double _256Ov2Pi = (256.0 / (2.0 * Math.PI));

   static {
      cvrtScaleDenom[CvType.CV_16U] = (0xffff);
      cvrtScaleDenom[CvType.CV_16S] = (0x7fff);
      cvrtScaleDenom[CvType.CV_8U] = (0xff);
      cvrtScaleDenom[CvType.CV_8S] = (0x7f);
   }

   public static class GradientImages {
      public final CvRaster gradientDir;
      public final CvRaster dx;
      public final CvRaster dy;

      private GradientImages(final CvRaster gradientDir, final CvRaster dx, final CvRaster dy) {
         this.gradientDir = gradientDir;
         this.dx = dx;
         this.dy = dy;
      }
   }

   public static CvRaster canny(final GradientImages gis, final double tlow, final double thigh, final Closer closer) {
      final CvMat edgeImage = new CvMat();
      gis.dx.matAp(dx -> gis.dy.matAp(dy -> Imgproc.Canny(dx, dy, edgeImage, tlow, thigh, true)));
      return CvRaster.toRaster(edgeImage, closer);
   }

   public static GradientImages gradient(final CvMat grayImage, final int kernelSize, final Closer closerp) {
      @SuppressWarnings("resource")
      final Closer closer = (closerp == null) ? new Closer() : closerp;

      // find gradient image
      final CvMat dx = new CvMat();
      final CvMat dy = new CvMat();
      Imgproc.Sobel(grayImage, dx, CvType.CV_16S, 1, 0, kernelSize, 1.0, 0.0, Core.BORDER_REPLICATE);
      final CvRaster dxr = CvRaster.toRaster(dx, closer);
      Imgproc.Sobel(grayImage, dy, CvType.CV_16S, 0, 1, kernelSize, 1.0, 0.0, Core.BORDER_REPLICATE);
      final CvRaster dyr = CvRaster.toRaster(dy, closer);
      final int numPixelsInGradient = dxr.rows() * dxr.cols();
      final byte[] dirsa = new byte[numPixelsInGradient];

      final short[] tmpdx = new short[numPixelsInGradient];
      dxr.matAp(m -> m.get(0, 0, tmpdx));

      try (ImageOpContext dxrIo = dxr.imageOp();
            ImageOpContext dyrIo = dyr.imageOp();) {
         for(int pos = 0; pos < numPixelsInGradient; pos++) {
            // calculate the angle
            final double dxv = ((short[])dxr.get(pos))[0];
            final double dyv = 0.0 - ((short[])dyr.get(pos))[0]; // flip y axis.
            dirsa[pos] = angle_byte(dxv, dyv);
         }
      }

      // a byte raster to hold the dirs
      final CvMat gradientDirImage = closer.add(CvRaster.create(dxr.rows(), dxr.cols(), CvType.CV_8UC1).disown());
      gradientDirImage.put(0, 0, dirsa);

      return new GradientImages(CvRaster.toRaster(gradientDirImage, closer), dxr, dyr);
   }

   public static CvMat convertToGray(final CvRaster origImage) {
      return convertToGray(origImage, null);
   }

   public static CvMat convertToGray(final CvRaster origImage, final Closer closer) {
      // convert to gray scale
      final CvMat grayImage = closer == null ? origImage.matOp(m -> convertToGray(m))
            : closer.add(origImage.matOp(m -> convertToGray(m)));
      return grayImage;
   }

   public static CvMat convertToGray(final CvMat src) {
      final CvMat workingImage = new CvMat();
      if(src.depth() != CvType.CV_8U) {
         System.out.print("converting image to 8-bit grayscale ... ");
         src.convertTo(workingImage, CvType.CV_8U, 255.0 / cvrtScaleDenom[src.depth()]);
         Imgproc.cvtColor(workingImage, workingImage, Imgproc.COLOR_BGR2GRAY);
         return workingImage;
      } else {
         src.copyTo(workingImage);
         Imgproc.cvtColor(src, workingImage, Imgproc.COLOR_BGR2GRAY);
         return workingImage;
      }
   }

   public static IndexColorModel getOverlayCM() {
      final byte[] r = new byte[256];
      final byte[] g = new byte[256];
      final byte[] b = new byte[256];

      r[intify(EDGE)] = g[intify(EDGE)] = b[intify(EDGE)] = -1;

      r[intify(ROVERLAY)] = -1;
      g[intify(GOVERLAY)] = -1;
      b[intify(BOVERLAY)] = -1;

      r[intify(YOVERLAY)] = -1;
      g[intify(YOVERLAY)] = -1;

      r[intify(COVERLAY)] = byteify(Color.cyan.getRed());
      g[intify(COVERLAY)] = byteify(Color.cyan.getGreen());
      b[intify(COVERLAY)] = byteify(Color.cyan.getBlue());

      r[intify(MOVERLAY)] = byteify(Color.magenta.getRed());
      g[intify(MOVERLAY)] = byteify(Color.magenta.getGreen());
      b[intify(MOVERLAY)] = byteify(Color.magenta.getBlue());

      r[intify(OOVERLAY)] = byteify(Color.orange.getRed());
      g[intify(OOVERLAY)] = byteify(Color.orange.getGreen());
      b[intify(OOVERLAY)] = byteify(Color.orange.getBlue());

      r[intify(GRAYOVERLAY)] = byteify(Color.gray.getRed());
      g[intify(GRAYOVERLAY)] = byteify(Color.gray.getGreen());
      b[intify(GRAYOVERLAY)] = byteify(Color.gray.getBlue());

      return new IndexColorModel(8, 256, r, g, b);
   }

   public static byte angle_byte(final double x, final double y) {
      double xu, yu, ang;
      double ret;
      int rret;

      xu = Math.abs(x);
      yu = Math.abs(y);

      if((xu == 0) && (yu == 0))
         return(0);

      ang = Math.atan(yu / xu);

      if(x >= 0) {
         if(y >= 0)
            ret = ang;
         else
            ret = (2.0 * Math.PI - ang);
      } else {
         if(y >= 0)
            ret = (Math.PI - ang);
         else
            ret = (Math.PI + ang);
      }

      rret = (int)(0.5 + (ret * _256Ov2Pi));
      if(rret >= 256)
         rret = 0;

      return byteify(rret);
   }

   private static byte byteify(final int i) {
      return i > 127 ? (byte)(i - 256) : (byte)i;
   }

   private static int intify(final byte b) {
      return (b < 0) ? (b) + 256 : (int)b;
   }

}
