package topology;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameRecorder;

import java.util.PriorityQueue;

/**
 * Created by Intern04 on 13/8/2014.
 */
public class StreamProducer implements Runnable {
    private PriorityQueue<StreamFrame> stream;
    private FrameRecorder recorder;
    final int firstFrameId, lastFrameId;
    private int nextExpectedFrame;
    private boolean finished;
    private CanvasFrame canvasFrame;

    final String FILENAME = "C:\\Users\\Intern04\\Downloads\\new\\Trash\\2.mp4";
    // [firstFrameId, lastFrameId)
    public StreamProducer(int firstFrameId, int lastFrameId) throws FrameRecorder.Exception {
        stream = new PriorityQueue<>();
        this.firstFrameId = firstFrameId;
        this.lastFrameId = lastFrameId;
        nextExpectedFrame = firstFrameId;

        recorder = FrameRecorder.createDefault(FILENAME, 728, 408);
        recorder.setFrameRate(25);
        recorder.setVideoQuality(1.0);
        recorder.start();

        canvasFrame = new CanvasFrame("View");
        finished = false;
    }


    public void addFrame(StreamFrame streamFrame) {
        synchronized (stream) {
            stream.add(streamFrame);
        }
    }

    public StreamFrame getNextFrame() {
        synchronized (stream) {
            if (stream.isEmpty() || stream.peek().frameId != nextExpectedFrame) return null;
            nextExpectedFrame ++;
            if (nextExpectedFrame == lastFrameId)
                finished = true;
            return stream.poll();
        }
    }


    @Override
    public void run() {
        while (!finished) {
            try {
                StreamFrame nextFrame = null;
                if ( (nextFrame = getNextFrame()) != null ) {
                    opencv_core.IplImage image = nextFrame.image.asIplImage();
                    recorder.record(image);
                    canvasFrame.showImage(image);
                    if (finished) {
                        recorder.stop();
                        recorder.release();
                        canvasFrame.dispose();
                    }
                } else {
                    Thread.sleep(40);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
    }
}
