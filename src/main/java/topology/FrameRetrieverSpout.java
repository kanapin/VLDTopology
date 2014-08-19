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
import static topology.Constants.RAW_FRAME_STREAM;
import static topology.Constants.PATCH_STREAM;


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
            if (Debug.timer)
                System.err.println("TIME=" + System.currentTimeMillis());
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
                Serializable.Mat sMat = new Serializable.Mat(new opencv_core.Mat(image));

                //TODO get params from config map
                double fx = .25, fy = .25;
                double fsx = .5, fsy = .5;

                int W = sMat.getCols(), H = sMat.getRows();
                int w = (int) (W * fx + .5), h = (int) (H * fy + .5);
                int dx = (int) (w * fsx + .5), dy = (int) (h * fsy + .5);
                int patchCount = 0;
                for (int x = 0; x + w <= W; x += dx)
                    for (int y = 0; y + h <= H; y += dy)
                        patchCount++;

                collector.emit(RAW_FRAME_STREAM, new Values(frameId, sMat, patchCount), frameId);
                for (int x = 0; x + w <= W; x += dx) {
                    for (int y = 0; y + h <= H; y += dy) {
                        Serializable.PatchIdentifier identifier = new
                                Serializable.PatchIdentifier(frameId, new Serializable.Rect(x, y, w, h));
                        collector.emit(PATCH_STREAM, new Values(identifier, patchCount), identifier.toString());
                    }
                }
                frameId ++;
                Thread.sleep(300);
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
        outputFieldsDeclarer.declareStream(PATCH_STREAM, new Fields("patchIdentifier", "patchCount"));
        outputFieldsDeclarer.declareStream(RAW_FRAME_STREAM, new Fields("frameId", "frameMat", "patchCount"));
    }


    

}
