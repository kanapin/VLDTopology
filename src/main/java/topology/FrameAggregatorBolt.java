package topology;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import logodetection.Debug;
import logodetection.Util;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameRecorder;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import static topology.StormConfigManager.getInt;

/**
 * Created by Intern04 on 5/8/2014.
 */
public class FrameAggregatorBolt extends BaseRichBolt {
    OutputCollector collector;

    StreamProducer producer;

    //int lim = 31685; // SONY
    int firstFrameId;
    int lastFrameId ;

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        firstFrameId = getInt(map, "firstFrameId");
        lastFrameId = getInt(map, "lastFrameId");
        this.collector = outputCollector;
        try {
            producer = new StreamProducer(firstFrameId, lastFrameId);
            new Thread(producer).start();
        } catch (FrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(Tuple tuple) {
        int frameId = (int)tuple.getValueByField("frameId");
        Serializable.Mat sMat = (Serializable.Mat)tuple.getValueByField("frameMat");
        List<Serializable.Rect> list = (List<Serializable.Rect>)tuple.getValueByField("foundRectList");

        opencv_core.Mat mat = sMat.toJavaCVMat();
        if (Debug.topologyDebugOutput)
            System.out.println("Frame " + frameId + " received " + (list == null ? 0 : list.size()) + " logos were found");

        if (list != null) {
            for (Serializable.Rect rect : list) {
                Util.drawRectOnMat(rect.toJavaCVRect(), mat, opencv_core.CvScalar.MAGENTA);
            }
        }

        producer.addFrame(new StreamFrame(frameId, mat, list));

        collector.ack(tuple);
    }
}
