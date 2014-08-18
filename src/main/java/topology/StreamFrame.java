package topology;

import org.bytedeco.javacpp.opencv_core;

import java.util.List;

/**
 * The class wrapping the output of the topology - a frame with a list of rectangles found on it.
 * frames are ordered by their frame id.
 */
public class StreamFrame implements Comparable<StreamFrame> {
    final public int frameId;
    final public opencv_core.Mat image;
    final public List<Serializable.Rect> detected;

    /**
     * creates a StreamFrame with given id, image matrix and list of rectangles corresponding to the detected logos.
     * @param frameId
     * @param image
     * @param detected
     */
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
