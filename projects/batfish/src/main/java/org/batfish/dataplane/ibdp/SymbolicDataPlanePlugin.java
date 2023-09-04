package org.batfish.dataplane.ibdp;

import com.google.auto.service.AutoService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.topology.TopologyProvider;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.IncrementalBdpAnswerElement;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.diagnosis.Diagnoser;

import java.util.Map;
import java.util.Set;

@AutoService(Plugin.class)
public final class SymbolicDataPlanePlugin extends DataPlanePlugin{
    private static final Logger LOGGER = LogManager.getLogger(IncrementalDataPlanePlugin.class);

    public static final String PLUGIN_NAME = "sebdp"; // symbolic execution data plane

    private SymbolicBdpEngine _engine;
    private Answer answer; // 针对这个answer(question)的symbolicExecution

    public SymbolicDataPlanePlugin() {}

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }
    public Answer getAnswer() {
        return answer;
    }
    @Override
    public ComputeDataPlaneResult computeDataPlane(NetworkSnapshot snapshot) {
        Map<String, Configuration> configurations = _batfish.loadConfigurations(snapshot);
        DataPlane dataPlane = _batfish.loadDataPlane(snapshot);
        // 此处构造Diagnoser，解析之前的dataplane数据，构造

        Set<BgpAdvertisement> externalAdverts =
                _batfish.loadExternalBgpAnnouncements(snapshot, configurations);

        LOGGER.info("Building topology for data-plane");
        TopologyProvider topologyProvider = _batfish.getTopologyProvider();
        TopologyContext topologyContext =
                TopologyContext.builder()
                        .setIpsecTopology(topologyProvider.getInitialIpsecTopology(snapshot))
                        .setIsisTopology(
                                IsisTopology.initIsisTopology(
                                        configurations, topologyProvider.getInitialLayer3Topology(snapshot)))
                        .setLayer3Topology(topologyProvider.getInitialLayer3Topology(snapshot))
                        .setLayer1Topologies(topologyProvider.getLayer1Topologies(snapshot))
                        .setL3Adjacencies(topologyProvider.getInitialL3Adjacencies(snapshot))
                        .setOspfTopology(topologyProvider.getInitialOspfTopology(snapshot))
                        .setTunnelTopology(topologyProvider.getInitialTunnelTopology(snapshot))
                        .build();

        ComputeDataPlaneResult answer =
                _engine.computeDataPlane(
                        configurations,
                        topologyContext,
                        externalAdverts,
                        topologyProvider.getInitialIpOwners(snapshot));
        _logger.infof(
                "Generated data-plane for snapshot:%s; iterations:%s",
                snapshot.getSnapshot(),
                ((IncrementalBdpAnswerElement) answer._answerElement).getDependentRoutesIterations());
        return answer;
    }

    @Override
    protected void dataPlanePluginInitialize() {
        _engine =
                new SymbolicBdpEngine(
                        new IncrementalDataPlaneSettings(_batfish.getSettingsConfiguration()));
    }

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }
}
