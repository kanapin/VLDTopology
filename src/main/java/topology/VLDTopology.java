package topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

/**
 * Created by Intern04 on 4/8/2014.
 */
public class VLDTopology {



    public static void main(String args[]) throws InterruptedException, AlreadyAliveException, InvalidTopologyException {
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("retriever", new FrameRetrieverSpout(), 1)
                .setNumTasks(1);

        builder.setBolt("processor", new PatchProcessorBolt(), 8)
                .shuffleGrouping("retriever", "patch-stream")
                .allGrouping("processor", "stream-to-patch-processor");

        builder.setBolt("intermediate", new PatchAggregatorBolt(), 1)
                .fieldsGrouping("processor", "stream-to-patch-aggregator", new Fields("frameId"));

        builder.setBolt("aggregator", new FrameAggregator(), 1)
                .globalGrouping("intermediate", "stream-to-frame-aggregator")
                .setNumTasks(1);

        StormTopology topology = builder.createTopology();
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("first", new Config(), topology);
        Thread.sleep(300*1000);
        cluster.killTopology("first");
        cluster.shutdown();
        //StormSubmitter.submitTopology("first", new Config(), topology);

    }
}
