package topology;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import logodetection.LogoTemplate;
import logodetection.Parameters;
import logodetection.StormVideoLogoDetector;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static topology.StormConfigManager.getListOfStrings;
import static topology.StormConfigManager.getString;

/**
 * Created by Intern04 on 5/8/2014.
 */
public class PatchProcessorBolt extends BaseRichBolt {
    OutputCollector collector;
    private StormVideoLogoDetector detector;
    HashSet<Serializable.PatchIdentifier> received;


    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
        // TODO: get path to logos & parameters from config
        Parameters parameters = new Parameters()
                .withMatchingParameters(
                        new Parameters.MatchingParameters()
                                .withMinimalNumberOfMatches(4)
                );

        List<String> templateFiles = getListOfStrings(map, "originalTemplateFileNames");
        detector = new StormVideoLogoDetector(parameters, templateFiles);
        received = new HashSet<>();
    }

    @Override
    public void execute(Tuple tuple) {
        if (tuple.getSourceStreamId().equals("stream-to-patch-processor") ) {
            Serializable.PatchIdentifier receivedPatchIdentifier = (Serializable.PatchIdentifier)tuple.getValueByField("hostPatchIdentifier");

            // TODO: This container could become very large, need to clear it after some time
            if ( !received.contains(receivedPatchIdentifier) )
            {
                received.add(receivedPatchIdentifier);
                Serializable.Mat mat = (Serializable.Mat) tuple.getValueByField("framePatchMat");

                detector.addTemplate(receivedPatchIdentifier, mat);

                Serializable.PatchIdentifier parent = (Serializable.PatchIdentifier)tuple.getValueByField("parentIdentifier");
                detector.incrementPriority(parent, 1);

                //System.out.println(Thread.currentThread().getId() + " thread: " + detector.getTemplateInfo());

            }

        } else {
            Serializable.PatchIdentifier patchIdentifier = (Serializable.PatchIdentifier)tuple.getValueByField("patchIdentifier");
            Serializable.Mat mat = (Serializable.Mat) tuple.getValueByField("frameMat");
            int patchCount = (int) tuple.getValueByField("patchCount");

            detector.detectLogosInRoi(mat.toJavaCVMat(), patchIdentifier.roi.toJavaCVRect(), patchIdentifier.frameId);
            Serializable.Rect foundRect = detector.getFoundRect();

            /* Notify other bolts that new logo template is added with priority 0*/
            if (foundRect != null) {

                Serializable.PatchIdentifier parentIdentifier = detector.getParentIdentifier();
                Serializable.Mat extractedTemplate = detector.getExtractedTemplate();
                // TODO: is anchoring really necessary?
                if (patchIdentifier != null && extractedTemplate != null && parentIdentifier != null) {

                    collector.emit("stream-to-patch-processor",
                            new Values(patchIdentifier, extractedTemplate, parentIdentifier));

                } else {
                    if (patchIdentifier == null)
                        System.err.println("[PatchProcessorBolt]:patchIdentifier is null!");
                    if (extractedTemplate == null)
                        System.err.println("[PatchProcessorBolt]:extractedTemplate is null!");
                    if (parentIdentifier == null)
                        System.err.println("[PatchProcessorBolt]:parentIdentifier is null!");
                }
            }

            /* Send this as well to allow Storm grouping tuples according to this field */
            int frameId = patchIdentifier.frameId;

            // TODO: is anchoring really necessary?
            collector.emit("stream-to-patch-aggregator", tuple,
                    new Values(frameId, patchIdentifier, foundRect, mat, patchCount));

        }
        collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream("stream-to-patch-aggregator",
                new Fields("frameId", "framePatchIdentifier", "foundRect", "frameMat", "patchCount"));

        outputFieldsDeclarer.declareStream("stream-to-patch-processor",
                new Fields("hostPatchIdentifier", "framePatchMat", "parentIdentifier"));
    }
}
