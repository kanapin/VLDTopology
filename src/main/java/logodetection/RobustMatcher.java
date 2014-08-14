package logodetection;

import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacv.JavaCV;
import topology.Serializable;

import java.util.ArrayList;

/**
 * Created by Intern04 on 8/8/2014.
 * Implementation of robust matcher described in OpenCV Cookbook ch. 9
 */
public class RobustMatcher {

    /* Parameters for detection */
    private Parameters params;

    private opencv_features2d.BFMatcher matcher;



    /* Lists of matches from template to patch, from patch to template, and matches they have in common.
       Used for symmetry test (see OpenCV cookbook Ch. 9)*/
    private opencv_features2d.DMatchVectorVector matches12, matches21, matches;

    /* The rectangle corresponding to detected logo */
    private Serializable.Rect foundRect;

    private Serializable.Mat extractedTemplate;

    /* Finds homography between two sets of keypoints */
    private opencv_core.Mat getHomography(opencv_features2d.KeyPoint logoKeyPoints, opencv_features2d.KeyPoint frameRegionKeyPoints) {

        /* First iteration: find homography matrix and outliers */
        int size = (int)matches.size();
        opencv_core.CvMat _src = opencv_core.cvCreateMat(size, 2, opencv_core.CV_32FC1);
        opencv_core.CvMat _dst = opencv_core.cvCreateMat(size, 2, opencv_core.CV_32FC1);
        for (int i = 0 ; i < size ; i ++) {
            int queryIndex = matches.get(i, 0).queryIdx();
            int trainIndex = matches.get(i, 0).trainIdx();
            opencv_core.Point2f logoPoint = logoKeyPoints.position(queryIndex).pt();
            opencv_core.Point2f frameRegionPoint = frameRegionKeyPoints.position(trainIndex).pt();
            logoKeyPoints.position(0);
            frameRegionKeyPoints.position(0);
            _src.put(i, 0, logoPoint.x());
            _src.put(i, 1, logoPoint.y());
            _dst.put(i, 0, frameRegionPoint.x());
            _dst.put(i, 1, frameRegionPoint.y());
        }
        opencv_core.Mat src = new opencv_core.Mat( _src );
        opencv_core.Mat dst = new opencv_core.Mat( _dst );
        opencv_core.Mat mask = new opencv_core.Mat(src.rows(), 1, opencv_core.TYPE_MASK);

        /* Information about outliers will be stored in mask */
        opencv_core.Mat h = opencv_calib3d.findHomography(src, dst, opencv_calib3d.RANSAC,
                params.getRansacParameters().getReprojectionThreshold(), mask);

        /* Second iteration: using only inliers */
        opencv_core.Mat __src = new opencv_core.Mat(0, 2, opencv_core.CV_32FC1);
        opencv_core.Mat __dst = new opencv_core.Mat(0, 2, opencv_core.CV_32FC1);

        opencv_core.CvMat cvMat = mask.asCvMat();
        for (int i = 0 ; i < cvMat.rows() ; i ++) {
            if (cvMat.get(i, 0) == 1.0) { // discard outliers
                __src.push_back(src.row(i));
                __dst.push_back(dst.row(i));
            }
        }

        mask = new opencv_core.Mat(__src.rows(), 1, opencv_core.TYPE_MASK);

        /* Find a homography matrix based on only inliers */
        h = opencv_calib3d.findHomography(__src, __dst, opencv_calib3d.RANSAC,
                params.getRansacParameters().getReprojectionThreshold(), mask);

        __src.release();
        __dst.release();
        mask.release();
        return h;
    }
    private void symmetryTest() {
        matches = new opencv_features2d.DMatchVectorVector();
        matches.resize(Math.max(matches12.size(), matches21.size()));
        int sz = 0;
        for (int i = 0 ; i < matches12.size() ; i ++) {
            for (int j = 0 ; j < matches21.size() ; j ++) {
                opencv_features2d.DMatch dMatch12 = matches12.get(i, 0), dMatch21 = matches21.get(j, 0);
                if (dMatch12.queryIdx() == dMatch21.trainIdx() && dMatch12.trainIdx() == dMatch21.queryIdx()) {
                    matches.resize(sz, 1);
                    matches.put(sz, 0, dMatch12);
                    sz ++;
                    break;
                }
            }
        }
        matches.resize(sz);
    }
    private opencv_features2d.DMatchVectorVector refineMatches(opencv_features2d.DMatchVectorVector oldMatches) {
        double RoD = params.getMatchingParameters().getRatioOfDistances();
        opencv_features2d.DMatchVectorVector newMatches = new opencv_features2d.DMatchVectorVector();

        // Refine results 1: Accept only those matches, where best dist is < RoD of 2nd best match.
        int sz = 0;
        newMatches.resize(oldMatches.size());

        double maxDist = 0.0, minDist = 1e100; // infinity

        for (int i = 0 ; i < oldMatches.size() ; i ++) {
            newMatches.resize(i, 1);
            if (oldMatches.get(i, 0).distance() < RoD * oldMatches.get(i, 1).distance()) {
                newMatches.put(sz, 0, oldMatches.get(i, 0));
                sz ++;
                double distance = oldMatches.get(i, 0).distance();
                if ( distance < minDist )
                    minDist = distance;
                if ( distance > maxDist )
                    maxDist = distance;
            }
        }
        //System.out.println("IN REFINE sz = " + sz + ", min = " + minDist + ", max = " + maxDist);
        newMatches.resize(sz);


        sz = 0;
        opencv_features2d.DMatchVectorVector brandNewMatches = new opencv_features2d.DMatchVectorVector();
        brandNewMatches.resize(newMatches.size());
        for (int i = 0 ; i < newMatches.size() ; i ++) {
//            System.out.println(newMatches.get(i, 0).distance());
            // TODO: Move this weights into params
            if (newMatches.get(i, 0).distance() <= 3 * minDist + maxDist / 12) { // Don't accept if too far from the best.
                brandNewMatches.resize(sz, 1);
                brandNewMatches.put(sz, 0, newMatches.get(i, 0));
                sz ++;
            }
        }
        //System.out.println("sz = " + sz);
        //newMatches.deallocate();
        brandNewMatches.resize(sz);
        return brandNewMatches;
        //return brandNewMatches;
    }

    public RobustMatcher(Parameters params) {
        matcher = new opencv_features2d.BFMatcher();
        this.params = params;
    }
    public boolean matchImages(opencv_core.Mat logoMat, opencv_core.Mat logoDescriptors, opencv_features2d.KeyPoint logoKeyPoints,
                                opencv_core.Mat frameRegionMat, opencv_core.Mat frameRegionDescriptors, opencv_features2d.KeyPoint frameRegionKeyPoints, opencv_core.Rect roi) {


        matches12 = new opencv_features2d.DMatchVectorVector();
        matches21 = new opencv_features2d.DMatchVectorVector();
        matcher.knnMatch(logoDescriptors, frameRegionDescriptors, matches12, 2); // Find only two best matches.
        matcher.knnMatch(frameRegionDescriptors, logoDescriptors, matches21, 2); // Find only two best matches.

        matches12 = refineMatches(matches12);
        matches21 = refineMatches(matches21);

        matches12.position(0);
        matches21.position(0);

        symmetryTest(); // updates matches using information from matches12 and matches 21

        /* Return false if too small number of matches defined in params */
        int size = (int)matches.size();
        if (size < params.getMatchingParameters().getMinimalNumberOfMatches()) {
            return false;
        }

        opencv_core.Mat homography = getHomography(logoKeyPoints, frameRegionKeyPoints);
        if (homography == null || homography.isNull()) {
            if (Debug.logoDetectionDebugOutput)
                System.out.println("No homography found");
            return false;
        }

        /* Choose rectangle where 90% of all matches lie in a logo */
        ArrayList<opencv_core.Point2f> kp = new ArrayList<opencv_core.Point2f>();
        for (int i = 0 ; i < matches.size(); i ++) {
            opencv_core.Point2f location = logoKeyPoints.position(matches.get(i, 0).queryIdx()).pt();
            kp.add(location);
            logoKeyPoints.position(0);
        }
        double[][] corners = new double[4][];
        opencv_core.Rect best = Util.bestBoundingBoxFast(kp, params.getMatchingParameters().getBoxAccuracy());
        double xMin = best.x(), yMin = best.y(), xMax = best.x() + best.width(), yMax = best.y() + best.height();

        // TODO: adjust this borders
        double dx = logoMat.cols() / 10.0, dy = logoMat.rows() / 10.0;
        xMin = Math.max(xMin - dx, 0.0);
        xMax = Math.min(xMax + dx, logoMat.cols() - 1);
        yMin = Math.max(yMin - dy, 0.0);
        yMax = Math.min(yMax + dy, logoMat.rows() - 1);
        corners[0] = new double[]{xMin, yMin};
        corners[1] = new double[]{xMax, yMin};
        corners[2] = new double[]{xMax, yMax};
        corners[3] = new double[]{xMin, yMax};

        double[][] scene_corners = new double[4][];
        for (int i = 0; i < 4; i++) {
            scene_corners[i] = new double[2];
            JavaCV.perspectiveTransform(corners[i], scene_corners[i], homography.asCvMat());
        }

        homography.release();

        if (Util.checkQuadrilateral(scene_corners, roi)) {

            xMin = 1e100; xMax = 0.0; yMin = 1e100; yMax = 0.0;
            for (int i = 0 ; i < 4; i ++) {
                double x = scene_corners[i][0] , y = scene_corners[i][1] ;
                if (xMin > x) xMin = x;
                if (xMax < x) xMax = x;
                if (yMin > y) yMin = y;
                if (yMax < y) yMax = y;
                scene_corners[i][0] += roi.x();
                scene_corners[i][1] += roi.y();
            }
            int xmn = Math.max(0, (int)xMin), xmx = Math.min(frameRegionMat.cols(), (int) xMax);
            int ymn = Math.max(0, (int)yMin), ymx =  Math.min(frameRegionMat.rows(), (int)yMax);
            foundRect = new Serializable.Rect(xmn + roi.x(), ymn + roi.y(), xmx - xmn, ymx - ymn);

            /* Prepare new logo template to push update to other bolts */
            opencv_core.Rect newRoi = new opencv_core.Rect(xmn, ymn, xmx - xmn, ymx - ymn);
            opencv_core.Mat _new = new opencv_core.Mat(frameRegionMat, newRoi);
            extractedTemplate = new Serializable.Mat(_new);
            /*
            IplImage _image = _new.clone().asIplImage();
            KeyPoint _kp = new KeyPoint();
            Mat _desc = new Mat();
            sift = new SIFT(0, 3, params.getSiftParameters().getContrastThreshold(),
                    params.getSiftParameters().getEdgeThreshold(), params.getSiftParameters().getSigma());
            sift.detectAndCompute(_new, Mat.EMPTY, _kp, _desc);
            */

            // Avoid Java's GC
            for (int i = 0 ; i < matches.size() ; i++)
                matches.get(i, 0).deallocate();
            matches.deallocate();
            return true;
        }
        for (int i = 0 ; i < matches.size() ; i++)
            matches.get(i, 0).deallocate();
        matches.deallocate();
        return false;
    }


    public Serializable.Mat getExtractedTemplate() {
        return extractedTemplate;
    }

    public Serializable.Rect getFoundRect() {
        return foundRect;
    }
}
