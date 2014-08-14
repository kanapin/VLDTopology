package topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

import static topology.StormConfigManager.getInt;
import static topology.StormConfigManager.readConfig;

import java.io.FileNotFoundException;

/**
 * Created by Intern04 on 4/8/2014.
 */
public class VLDTopology {



    public static void main(String args[]) throws InterruptedException, AlreadyAliveException, InvalidTopologyException, FileNotFoundException {

        Config conf = readConfig("config.yaml");

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("retriever", new FrameRetrieverSpout(), getInt(conf, "FrameRetrieverSpout.parallelism"))
                .setNumTasks(getInt(conf, "FrameRetrieverSpout.tasks"));




        builder.setBolt("processor", new PatchProcessorBolt(), getInt(conf, "PatchProcessorBolt.parallelism"))
                .shuffleGrouping("retriever", "patch-stream")
                .allGrouping("processor", "stream-to-patch-processor")
                .setNumTasks(getInt(conf, "PatchProcessorBolt.tasks"));

        builder.setBolt("intermediate", new PatchAggregatorBolt(), getInt(conf, "PatchAggregatorBolt.parallelism"))
                .fieldsGrouping("processor", "stream-to-patch-aggregator", new Fields("frameId"))
                .setNumTasks(getInt(conf, "PatchAggregatorBolt.tasks"));

        builder.setBolt("aggregator", new FrameAggregatorBolt(), getInt(conf, "FrameAggregatorBolt.parallelism"))
                .globalGrouping("intermediate", "stream-to-frame-aggregator")
                .setNumTasks(getInt(conf, "FrameAggregatorBolt.tasks"));

        StormTopology topology = builder.createTopology();
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("first", conf, topology);
        Thread.sleep(300*1000);
        cluster.killTopology("first");
        cluster.shutdown();
        //StormSubmitter.submitTopology("first", new Config(), topology);

    }
}
