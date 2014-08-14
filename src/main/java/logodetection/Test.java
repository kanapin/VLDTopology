package logodetection;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.*;
import topology.Serializable;

/**
 * Created by Intern04 on 6/8/2014.
 */
public class Test {


    public static void main(String args[]) throws FrameRecorder.Exception, FrameGrabber.Exception {
        final String SOURCE_FILE = "C:/Users/Intern04/Downloads/new/VideoLogoDetector1.2/1.mp4";
        final String LOGO_FILE = "C:/Users/Intern04/Downloads/new/VideoLogoDetector1.2/sony.jpg";
        FrameGrabber grabber;
        grabber = new OpenCVFrameGrabber(SOURCE_FILE);
        int frameId = 0;
        opencv_core.IplImage image;
        Parameters parameters = new Parameters()
                .withMatchingParameters(
                        new Parameters.MatchingParameters()
                                .withMinimalNumberOfMatches(4)
                );
        StormVideoLogoDetector detector = new StormVideoLogoDetector(parameters, LOGO_FILE);

        CanvasFrame canvas = new CanvasFrame("VideoCanvas");

        // Set Canvas frame to close on exit
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        FrameRecorder recorder = FrameRecorder.createDefault("2.mp4",  728, 408);







        try {
            grabber.start();
            recorder.setFrameRate(25);
            recorder.start();

            System.out.println("Grabber & Recorder started");
            int lim = 31685; // SONY
            while (frameId++ < lim)
                grabber.grab();

            canvas.setCanvasSize(grabber.getImageWidth(),
                    grabber.getImageHeight());

            while (frameId ++ < lim + 2000) {
                image = grabber.grab();

                opencv_core.Mat frameMat = new opencv_core.Mat(image);
                opencv_core.Mat finalMat = frameMat.clone();

                //TODO get params from config map
                double fx = .25, fy = .25;
                double fsx = .33, fsy = .33;

                int W = frameMat.cols(), H = frameMat.rows();
                int w = (int) (W * fx + .5), h = (int) (H * fy + .5);
                int dx = (int) (w * fsx + .5), dy = (int) (h * fsy + .5);

                for (int x = 0; x + w <= W; x += dx) {
                    for (int y = 0; y + h <= H; y += dy) { // N23@150,120-250,240
                        detector.detectLogosInRoi(frameMat, new opencv_core.Rect(x, y, w, h), frameId);
                        Serializable.Rect foundRect = detector.getFoundRect();
                        if (foundRect != null)
                            Util.drawRectOnMat(foundRect.toJavaCVRect(), finalMat, opencv_core.CvScalar.MAGENTA);
                    }
                }
                opencv_core.IplImage iplImage = finalMat.asIplImage();
                canvas.showImage(iplImage);


                System.out.println(iplImage.width() + " " + iplImage.height());
                recorder.record(iplImage);

                System.out.println(iplImage.width() + " " + iplImage.height());
            }
            recorder.stop();

        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }


}
