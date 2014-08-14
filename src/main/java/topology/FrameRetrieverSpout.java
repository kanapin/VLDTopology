package topology;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import logodetection.Debug;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.util.Map;

import static topology.StormConfigManager.getInt;
import static topology.StormConfigManager.getString;


/**
 * Created by Intern04 on 4/8/2014.
 */
public class FrameRetrieverSpout extends BaseRichSpout {
    SpoutOutputCollector collector;
    private String SOURCE_FILE;
    private FrameGrabber grabber;
    private int frameId;

    int firstFrameId ;
    int lastFrameId ;

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {

        frameId = 0;
        firstFrameId = getInt(map, "firstFrameId");
        lastFrameId = getInt(map, "lastFrameId");
        SOURCE_FILE = getString(map, "videoSourceFile");

        this.collector = spoutOutputCollector;
        grabber = new OpenCVFrameGrabber(SOURCE_FILE);

        try {
            grabber.start();
            if (Debug.topologyDebugOutput)
                System.out.println("Grabber started");

            while (++frameId < firstFrameId)
                grabber.grab();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    opencv_core.IplImage image;
    @Override
    public void nextTuple() {
        try {
            if (frameId < lastFrameId) {
                image = grabber.grab();
                opencv_core.Mat frameMat = new opencv_core.Mat(image);
                Serializable.Mat sMat = new Serializable.Mat(frameMat);

                //TODO get params from config map
                double fx = .25, fy = .25;
                double fsx = .33, fsy = .33;

                int W = frameMat.cols(), H = frameMat.rows();
                int w = (int) (W * fx + .5), h = (int) (H * fy + .5);
                int dx = (int) (w * fsx + .5), dy = (int) (h * fsy + .5);
                int patchCount = 0;
                for (int x = 0; x + w <= W; x += dx)
                    for (int y = 0; y + h <= H; y += dy)
                        patchCount++;

                for (int x = 0; x + w <= W; x += dx) {
                    for (int y = 0; y + h <= H; y += dy) { // N23@150,120-250,240
                        Serializable.PatchIdentifier identifier = new
                                Serializable.PatchIdentifier(frameId, new Serializable.Rect(x, y, w, h));

                        collector.emit("patch-stream", new Values(identifier, sMat, patchCount), identifier.toString());
                    }
                }
                frameId ++;
                Thread.sleep(500);
            }

        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Someone (perhaps you) has shut down this thread");
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream("patch-stream", new Fields("patchIdentifier",
                "frameMat", "patchCount"));
    }


    

}
