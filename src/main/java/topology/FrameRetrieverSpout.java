package topology;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import logodetection.Debug;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_highgui;
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
    //private FrameGrabber grabber;
    private opencv_highgui.VideoCapture capture;
    private int frameId;
    private long lastFrameTime;

    int firstFrameId ;
    int lastFrameId ;
    opencv_core.Mat mat;

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {

        capture = new opencv_highgui.VideoCapture(SOURCE_FILE);
        frameId = 0;
        firstFrameId = getInt(map, "firstFrameId");
        lastFrameId = getInt(map, "lastFrameId");
        SOURCE_FILE = getString(map, "videoSourceFile");
        mat = new opencv_core.Mat();

        this.collector = spoutOutputCollector;

        if (Debug.topologyDebugOutput)
            System.out.println("Grabber started");

        while (++frameId < firstFrameId)
            capture.grab();
        if (Debug.timer)
            System.err.println("TIME=" + System.currentTimeMillis());

    }


    @Override
    public void nextTuple() {
        long now = System.currentTimeMillis();
        if (now - lastFrameTime < 1000){
            return;
        }else {
            lastFrameTime=now;
        }

        if (frameId < lastFrameId) {
            capture.read(mat);

            Serializable.Mat sMat = new Serializable.Mat(mat);

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
        }


    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream(PATCH_STREAM, new Fields("patchIdentifier", "patchCount"));
        outputFieldsDeclarer.declareStream(RAW_FRAME_STREAM, new Fields("frameId", "frameMat", "patchCount"));
    }


    

}
