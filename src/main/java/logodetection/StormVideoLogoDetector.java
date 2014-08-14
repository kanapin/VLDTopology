package logodetection;
/*** ==============================Definitions==========================
 * 1. Query image, query logo, logo template - images of logos we are looking for.
 *
 * 2. Train image, frame image - image of the frame in which we are looking for logos.
 *
 * 3. Homography matrix, homography - 3x3 transformation matrix, which projects our logo template onto the
 *    frame space. Usually we project the corners of our template logo onto a frame to obtain the
 *    quadrilateral, which determines the locations of the detected logo.
 *
 * 4. RoI, roi - region of interest. Usually this is the rectangle corresponding to the part of the image
 *    which we are examining.
 *
 *
 *
 *
 *
 ***/

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_features2d.BFMatcher;
import org.bytedeco.javacpp.opencv_features2d.DMatchVectorVector;
import org.bytedeco.javacpp.opencv_features2d.KeyPoint;
import org.bytedeco.javacpp.opencv_nonfree.SIFT;
import org.bytedeco.javacv.JavaCV;
import topology.Serializable;

/**
 * Created by nurlan on 8/05/14.
 */
public class StormVideoLogoDetector {

    /* These are for storing Images of searched logos, their key points and their descriptors */
    private ArrayList<LogoTemplate> originalTemplates;
    private ArrayList<LogoTemplate> addedTemplates;

    /* Parameters for detection */
    private Parameters params;
    private SIFT sift;
    private RobustMatcher robustMatcher;

    /* The rectangle corresponding to detected logo */
    private Serializable.Rect foundRect;
    private Serializable.Mat extractedTemplate;
    private LogoTemplate parent;

    /* Initialize and precompute all key points, descriptors for template logos */
    public StormVideoLogoDetector(Parameters params, String... fileNames) {
        if (Debug.printDebug)
            System.out.println("Initializing logos...");

        this.params = params;
        originalTemplates = new ArrayList<LogoTemplate>();
        addedTemplates = new ArrayList<LogoTemplate>();
        robustMatcher = new RobustMatcher(params);

        // TODO: if too few features are detected, tune coefficients of sift and retry.
        sift = new SIFT(0, 3, params.getSiftParameters().getContrastThreshold(),
                params.getSiftParameters().getEdgeThreshold(), params.getSiftParameters().getSigma());

        for (int i = 0 ; i < fileNames.length ; i ++)
        {
            try
            {
                Mat tmp = new Mat(IplImage.createFrom(ImageIO.read(new FileInputStream(fileNames[i]))));

                Mat descriptor = new Mat();
                KeyPoint keyPoints = new KeyPoint();
                sift.detectAndCompute(tmp, Mat.EMPTY, keyPoints, descriptor);
                // TODO check if template logo has enough descriptors & matches
                // TODO original templates have negative ids and null roi.
                originalTemplates.add(new LogoTemplate(tmp, keyPoints, descriptor, new Serializable.PatchIdentifier(-i - 1, null)));
            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.err.println("StormLogoDetector(): Could not open file " + fileNames[i]);
            }
        }
        if (Debug.printDebug)
            System.out.println("Initialization of logo templates complete.");
    }




    public void detectLogosInRoi(Mat frame, Rect roi, int frameId) {

        foundRect = null;
        Mat r = new Mat(frame, roi);
        KeyPoint keyPoints = new KeyPoint();
        Mat testDescriptors = new Mat();
        // make r continuous
        r = r.clone();

        sift.detectAndCompute(r, Mat.EMPTY, keyPoints, testDescriptors);

        Collections.sort(originalTemplates);
        for (LogoTemplate lt : originalTemplates) {
            if (keyPoints.capacity() >= params.getMatchingParameters().getMinimalNumberOfMatches() &&
                    robustMatcher.matchImages(lt.imageMat, lt.descriptor, lt.keyPoints,
                            r, testDescriptors, keyPoints, roi))
            {
                parent = lt;
                foundRect = robustMatcher.getFoundRect();
                extractedTemplate = robustMatcher.getExtractedTemplate();
                break;
            }
        }

        if (foundRect == null) { // If logo hasn't been yet found

            Collections.sort(addedTemplates);
            if (addedTemplates.size() > 5)
                addedTemplates.remove(addedTemplates.size() - 1);

            for (LogoTemplate lt : addedTemplates) {
                if (keyPoints.capacity() >= params.getMatchingParameters().getMinimalNumberOfMatches() &&
                        robustMatcher.matchImages(lt.imageMat, lt.descriptor, lt.keyPoints,
                                r, testDescriptors, keyPoints, roi))
                {
                    parent = lt;
                    foundRect = robustMatcher.getFoundRect();
                    extractedTemplate = robustMatcher.getExtractedTemplate();
                    break;
                }
            }
        }

        r.release();
        keyPoints.deallocate();
        testDescriptors.release();
    }

    // This I added trying to get rid of javacv's bug
    public void finish() {
        sift.deallocate();
    }

    public Serializable.Rect getFoundRect() {
        return foundRect;
    }

    public Serializable.Mat getExtractedTemplate() {
        return extractedTemplate;
    }

    public Serializable.PatchIdentifier getParentIdentifier() {
        return parent.identifier;
    }
    public boolean incrementPriority(Serializable.PatchIdentifier identifier, int value) {
        for ( LogoTemplate lt : originalTemplates) {
            if (lt.identifier.equals(identifier)) {
                lt.priority += value;
                return true;
            }
        }
        for ( LogoTemplate lt : addedTemplates) {
            if (lt.identifier.equals(identifier)) {
                lt.priority += value;
                return true;
            }
        }
        return false;
    }
    public void addTemplate(Serializable.PatchIdentifier identifier, Serializable.Mat mat) {
        if (addedTemplates.size() >= 5)
            return;
        Mat image = mat.toJavaCVMat();
        Mat descriptor = new Mat();
        KeyPoint keyPoints = new KeyPoint();
        sift.detectAndCompute(image, Mat.EMPTY, keyPoints, descriptor);
        addedTemplates.add(new LogoTemplate(image, keyPoints, descriptor, identifier));

    }
    public String getTemplateInfo() {
        return "" + originalTemplates + ", " + addedTemplates;
    }
}
