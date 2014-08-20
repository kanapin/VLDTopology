package topology;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import logodetection.Debug;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_features2d;
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
    private FrameGrabber grabber;
    //private opencv_highgui.VideoCapture capture;
    private int frameId;
    private long lastFrameTime;

    int firstFrameId ;
    int lastFrameId ;

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {

        frameId = 0;
        firstFrameId = getInt(map, "firstFrameId");
        lastFrameId = getInt(map, "lastFrameId");
        SOURCE_FILE = getString(map, "videoSourceFile");
        //capture = new opencv_highgui.VideoCapture(SOURCE_FILE);
        grabber = new OpenCVFrameGrabber(SOURCE_FILE);
        opencv_features2d.KeyPoint kp = new opencv_features2d.KeyPoint();
        System.out.println("Created capture: " + SOURCE_FILE);


        this.collector = spoutOutputCollector;
        try {
            grabber.start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }

        kp.deallocate();

        if (Debug.topologyDebugOutput)
            System.out.println("Grabber started");

        while (++frameId < firstFrameId)
            try {
                grabber.grab();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        if (Debug.timer)
            System.out.println("TIME=" + System.currentTimeMillis());

    }

    opencv_core.IplImage image;
    opencv_core.Mat mat;
    @Override
    public void nextTuple() {
        long now = System.currentTimeMillis();
        if (now - lastFrameTime < 1000){
            return;
        }else {
            lastFrameTime=now;
        }


        if (frameId < lastFrameId) {
            try {
                image = grabber.grab();
                mat = new opencv_core.Mat(image);
                System.out.println("Current frame = " + frameId);
                System.out.println("Mat mat: rows() = " + mat.rows() + ", cols() = " + mat.cols() + ", mat.type() = " + mat.type());

                Serializable.Mat sMat = new Serializable.Mat(mat);

                System.out.println("Serializable.Mat mat: rows() = " + sMat.getRows() + ", cols() = " + sMat.getCols() +
                        ", mat.type() = " + sMat.getType());

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
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }


        }


    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream(PATCH_STREAM, new Fields("patchIdentifier", "patchCount"));
        outputFieldsDeclarer.declareStream(RAW_FRAME_STREAM, new Fields("frameId", "frameMat", "patchCount"));
    }


    

}
