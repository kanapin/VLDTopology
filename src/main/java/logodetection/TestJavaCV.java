package logodetection;

import org.bytedeco.javacpp.*;
import topology.Serializable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Intern04 on 4/8/2014.
 */
public class TestJavaCV {
    public static void main(String args[]) {
        try {
            opencv_features2d.KeyPoint kp = new opencv_features2d.KeyPoint();
            opencv_core.Mat img = new opencv_core.Mat(opencv_core.IplImage.createFrom(ImageIO.read(new File("mc2.jpg"))));

            opencv_core.Rect roi = new opencv_core.Rect(200, 100, 50, 60);
            opencv_core.Mat part = new opencv_core.Mat(img, roi);


            Serializable.Mat sMat = new Serializable.Mat(part);

            opencv_core.Mat _img = sMat.toJavaCVMat();

            ImageIO.write(_img.getBufferedImage(), "png", new File("mc2part.png"));






        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
