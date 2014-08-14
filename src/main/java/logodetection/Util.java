package logodetection;

import org.bytedeco.javacpp.opencv_core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Nurlan Kanapin on 25/7/2014.
 */
public class Util {
    public static void drawRectOnMat(opencv_core.Rect r, opencv_core.Mat finalImage, opencv_core.CvScalar scalar) {
        opencv_core.Scalar color = new opencv_core.Scalar(scalar);
        opencv_core.Point A = new opencv_core.Point(r.x(), r.y()), B = new opencv_core.Point(r.x() + r.width(), r.y()),
                C = new opencv_core.Point(r.x() + r.width(), r.y()+ r.height()), D = new opencv_core.Point(r.x(), r.y() + r.height());
        opencv_core.line(finalImage, A, B, color, 4, 4, 0);
        opencv_core.line(finalImage, B, C, color, 4, 4, 0);
        opencv_core.line(finalImage, C, D, color, 4, 4, 0);
        opencv_core.line(finalImage, D, A, color, 4, 4, 0);
    }
    public static void drawQonMat(double [][] Q, opencv_core.Mat finalImage, opencv_core.CvScalar scalar) {
        opencv_core.Scalar color = new opencv_core.Scalar(scalar);
        for (int i = 0; i < 4; i++) {
            opencv_core.line(finalImage, new opencv_core.Point((int) Q[i][0], (int) Q[i][1]),
                    new opencv_core.Point((int) Q[(i + 1) % 4][0], (int) Q[(i + 1) % 4][1]), color, 4, 4, 0);

        }
    }
    final static double eps = 1e-2;
    public static boolean isConvex(double [][] corners) {
        assert corners.length == 4;
        for (int i = 0 ; i < 4 ; i ++)
            assert corners[i].length == 2;
        for (int i = 0 ; i < 4 ; i ++) { // vprev * vnext > 0
            int prev = (i - 1 + 4) % 4, next = ( i + 1 ) % 4;
            if (CCW(corners[prev], corners[i], corners[next]))
                return false;

        }
        return true;
    }
    public static boolean CCW(double[] a, double[] b, double[] c) {
        double x1 = a[0] - b[0], y1 = a[1] - b[1];
        double x2 = c[0] - b[0], y2 = c[1] - b[1];
        return x1 * y2 - x2 * y1 > eps;
    }
    public static double area(double [][] p) {
        double res = 0.0;
        for (int i = 0 ; i < 4 ; i ++) {
            res += p[i][0] * p[(i+1)%4][1] - p[(i+1)%4][0] * p[i][1];
        }
        return Math.abs(res);
    }
    public static boolean checkQuadrilateral(double [][] scene_corners, opencv_core.Rect roi) {
        double xMax = 0.0, xMin = 1e100;
        double yMax = 0.0, yMin = 1e100;
        for (int i = 0 ; i < 4 ; i ++) {
            xMax = Math.max(xMax, scene_corners[i][0]);
            xMin = Math.min(xMin, scene_corners[i][0]);
            yMax = Math.max(yMax, scene_corners[i][1]);
            yMin = Math.min(yMin, scene_corners[i][1]);
        }

        // TODO: update 07/29. If opposite sides are too far from being a rectangle
        /*
        double [] sides = new double [4];
        for (int i = 0 ; i < 4 ; i ++) {
            sides[i] = (scene_corners[i][0] - scene_corners[(i+1)%4][0])*(scene_corners[i][0] - scene_corners[(i+1)%4][0]);
        }
        for (int i = 0 ; i < 2 ; i ++)
            if (sides[i] / sides[i + 2] > 3.0 || sides[i] / sides[i + 2] < 1.0 / 3) {
                System.out.println("Too squeezed");
                return false;
            }

        */

        // TODO: adjust these constants.
        if (xMax - xMin < 10 || yMax - yMin < 10) {
            if (Debug.logoDetectionDebugOutput)
                System.out.println("Of too small resolution");
            return false;
        }
        double wx = xMax - xMin, hy = yMax - yMin;

        // TODO: update 07/21. If the quadrilateral is too large with respect to roi, it is counted as bad
        if (wx > 2 * roi.width() || hy > 2 * roi.height()) {
            if (Debug.logoDetectionDebugOutput)
                System.out.println("Too large");
            return false;
        }


        // TODO: update 07/21 If quadrilateral is too 'flattened' return false (Area < C * Bounding_box_area), C = 1/2
        if (Util.area(scene_corners) < .5 * (xMax - xMin) * (yMax - yMin)) {
            if (Debug.logoDetectionDebugOutput)
                System.out.println("Of too small area");
            return false;
        }
        if (!Util.isConvex(scene_corners)) {
            if (Debug.logoDetectionDebugOutput)
                System.out.println("not a convex");
            return false;
        }
        return true;
    }
    public static opencv_core.Rect bestBoundingBoxFast(ArrayList<opencv_core.Point2f> list, double accuracy) {
        if (accuracy < 0.0 || accuracy > 1.0) {
            System.err.println("Not valid accuracy [0.0, 1.0]");
            accuracy = .9;
        }
        int N = list.size(), n = (int)Math.ceil(N * accuracy);
        opencv_core.Rect best = new opencv_core.Rect(0, 0, 1 << 15, 1 << 15); // 'infinite' rectangle

        ArrayList<opencv_core.Point2f> xSorted = (ArrayList<opencv_core.Point2f>)list.clone();
        ArrayList<opencv_core.Point2f> ySorted = (ArrayList<opencv_core.Point2f>)list.clone();

        Collections.sort(xSorted, new Comparator<opencv_core.Point2f>() {
            public int compare(opencv_core.Point2f o1, opencv_core.Point2f o2) {
                if (o1.x() < o2.x()) return -1;
                if (o1.x() > o2.x()) return 1;
                if (o1.y() < o2.y()) return -1;
                if (o1.y() > o2.y()) return 1;
                return 0;
            }
        });
        Collections.sort(ySorted, new Comparator<opencv_core.Point2f>() {
            public int compare(opencv_core.Point2f o1, opencv_core.Point2f o2) {
                if (o1.y() < o2.y()) return -1;
                if (o1.y() > o2.y()) return 1;
                if (o1.x() > o2.x()) return -1;
                if (o1.x() < o2.x()) return 1;
                return 0;
            }
        });

        for (int i = 0 ; i < N ; i ++) {
            for (int j = 0 ; j < N ; j ++) {
    			/*
    			int cur = 0, pup = N - 1, pright = i;
    			while (cur < n && pright < N) {
    				if (xSorted.get(pright).y() >= ySorted.get(j).y())
    					cur ++;
    				pright ++;
    			}
    			if (cur < n) break;// ????
    			double xMin = xSorted.get(i).x(), xMax = xSorted.get(pright).x(),
						yMin = ySorted.get(j).y(), yMax = ySorted.get(pup).y();
				*/
                int cur = 0;
                double xMin = xSorted.get(i).x(),
                        yMin = ySorted.get(j).y();
                for (int pup = N - 1, pright = i - 1; pup >= j ;  ) {
                    while ( pright + 1 < N &&  (cur < n || (pright < 0 || xSorted.get(pright).x() == xSorted.get(pright + 1).x()) )  ) {
                        if (xSorted.get(pright + 1).y() >= ySorted.get(j).y() && xSorted.get(pright + 1).y() <= ySorted.get(pup).y())
                            cur ++;
                        pright = pright + 1;
                    }

                    if (cur < n) break;// ????

                    double xMax = xSorted.get(pright).x();
                    double yMax = ySorted.get(pup).y();
                    if ((xMax - xMin) * (yMax - yMin) < best.area()) {
                        best = new opencv_core.Rect((int)xMin, (int)yMin, (int)(xMax - xMin), (int)(yMax - yMin));
                    }
                    if ( ySorted.get(pup).x() >= xMin && ySorted.get(pup).x() <= xMax) {
                        cur --;
                    }

                    pup --;
                }
            }
        }
        return best;
    }
}
