package test;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.junit.Test;

/**
 * Created by nurlan on 8/20/14.
 */
public class GrabberTest {
    @Test
    public void createGrabber() throws FrameGrabber.Exception {
        String SOURCE_FILE = "/Users/nurlan/Desktop/1.mp4";
        opencv_highgui.VideoCapture capture = new opencv_highgui.VideoCapture(SOURCE_FILE);
        opencv_core.Mat image = new opencv_core.Mat();
        int id = 0;
        while (id < 100) {
            capture.read(image);
        }
        capture.release();
    }

}
