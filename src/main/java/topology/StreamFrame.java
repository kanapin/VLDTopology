package topology;

import org.bytedeco.javacpp.opencv_core;

import java.util.List;

/**
 * Created by Intern04 on 13/8/2014.
 */
public class StreamFrame implements Comparable<StreamFrame> {
    final public int frameId;
    final public opencv_core.Mat image;
    final public List<Serializable.Rect> detected;

    public StreamFrame(int frameId, opencv_core.Mat image, List<Serializable.Rect> detected) {
        this.frameId = frameId;
        this.image = image;
        this.detected = detected;
    }

    @Override
    public int compareTo(StreamFrame o) {
        return frameId - o.frameId;
    }
}
