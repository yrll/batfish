package org.batfish.datamodel.bgp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
//import com.google.common.graph.*;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.common.graph.ImmutableValueGraph;
import org.batfish.common.topology.ValueEdge;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Comparator;

/** A topology representing all BGP peerings. */
@ParametersAreNonnullByDefault
public final class BgpTopology {

  public static final BgpTopology EMPTY =
      new BgpTopology(ValueGraphBuilder.directed().allowsSelfLoops(false).build());
  private static final String PROP_EDGES = "edges";
  private static final String PROP_NODES = "nodes";

  @JsonCreator
  private static @Nonnull BgpTopology create(
      @JsonProperty(PROP_EDGES) @Nullable
          List<ValueEdge<BgpPeerConfigId, BgpSessionProperties>> edges,
      @JsonProperty(PROP_NODES) @Nullable Set<BgpPeerConfigId> nodes) {
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> graph =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    if (nodes != null) {
      nodes.forEach(graph::addNode);
    }
    if (edges != null) {
      edges.forEach(
          valueEdge ->
              graph.putEdgeValue(
                  valueEdge.getSource(), valueEdge.getTarget(), valueEdge.getValue()));
    }
    return new BgpTopology(graph);
  }

  private final ValueGraph<BgpPeerConfigId, BgpSessionProperties> _graph;

  public BgpTopology(ValueGraph<BgpPeerConfigId, BgpSessionProperties> graph) {
    _graph = ImmutableValueGraph.copyOf(graph);
  }

  @JsonProperty(PROP_EDGES)
  private List<ValueEdge<BgpPeerConfigId, BgpSessionProperties>> getEdges() {
    return _graph.edges().stream()
        .map(
            endpointPair ->
                new ValueEdge<>(
                    endpointPair.source(),
                    endpointPair.target(),
                    _graph.edgeValue(endpointPair.source(), endpointPair.target()).get()))
        .collect(ImmutableList.toImmutableList());
  }

  @JsonIgnore
  public @Nonnull ValueGraph<BgpPeerConfigId, BgpSessionProperties> getGraph() {
    return _graph;
  }

  @JsonProperty(PROP_NODES)
  private @Nonnull Set<BgpPeerConfigId> getNodes() {
    return _graph.nodes();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BgpTopology)) {
      return false;
    }
    return _graph.equals(((BgpTopology) obj)._graph);
  }

  @Override
  public int hashCode() {
    return _graph.hashCode();
  }

  /**
   * get all peers of the input peer
   */
  public Set<BgpPeerConfigId> getPeers(BgpPeerConfigId curPeer) {
    Set<BgpPeerConfigId> peers = new HashSet<>();
    _graph.edges().forEach(edge->{
      if (edge.source().equals(curPeer)) {
        peers.add(edge.target());
      } else if (edge.target().equals(curPeer)) {
        peers.add(edge.source());
      }
    });
    return peers;
  }

  public BgpPeerConfigId getTheFirstPeerConfigId(String node) {
    for (BgpPeerConfigId bgpPeerConfigId: _graph.nodes()) {
      if (bgpPeerConfigId.getHostname().equals(node)) {
        return bgpPeerConfigId;
      }
    }
    return null;
  }

  /**
   * get all peers of the input peer
   */
  public Set<String> getPeers(String node) {
    Set<BgpPeerConfigId> peers = getPeers(getTheFirstPeerConfigId(node));
    Set<String> peersName = new HashSet<>();
    peers.forEach(p->peersName.add(p.getHostname()));
    return peersName;
  }

  /**
   * get the AS number of "thisNode" (towards a specific peerNode)
   * */
  public long getAsNumber(String thisNode, String peerNode) {
    for (EndpointPair<BgpPeerConfigId> edge: _graph.edges()) {
      if (edge.target().getHostname().equals(peerNode) && edge.source().getHostname().equals(thisNode)) {
        return  _graph.edgeValue(edge).get().getLocalAs();
      }
    }
    return Long.parseLong(null);
  }

  /**
   * get the bgpPeerConfigIp of "thisNode" (towards a specific peerNode)
   * */
  public BgpPeerConfigId getBgpPeerConfigId(String thisNode, String peerNode) {
    for (EndpointPair<BgpPeerConfigId> edge: _graph.edges()) {
      if (edge.target().getHostname().equals(peerNode) && edge.source().getHostname().equals(thisNode)) {
        return  edge.source();
      }
    }
    return null;
  }

  /**
   * get the sessionProp of a pair of nodes
   */
  public BgpSessionProperties getBgpSessionProp(String thisNode, String peerNode) {
    for (EndpointPair<BgpPeerConfigId> edge: _graph.edges()) {
      if (edge.target().getHostname().equals(peerNode) && edge.source().getHostname().equals(thisNode)) {
        return  _graph.edgeValue(edge).get();
      }
    }
    return null;
  }

  /**
   * get peers toward specif session type
   */
  public Set<String> getBgpPeers(String node, Set<String> otherNodes, boolean isEbgp) {
    Set<String> resNodes = new HashSet<>();
    if (otherNodes!=null) {
      for (String otherNode: otherNodes) {
            BgpSessionProperties session = getBgpSessionProp(node, otherNode);
            if (session!=null && session.isEbgp()==isEbgp) {
              resNodes.add(otherNode);
            }
          }
    }

    return resNodes;
  }

  /** Directional, reversible BGP edge pointing to two {@link BgpPeerConfigId}. */
  @ParametersAreNonnullByDefault
  public static final class EdgeId implements Comparable<EdgeId> {

    @Nonnull private final BgpPeerConfigId _tail;
    @Nonnull private final BgpPeerConfigId _head;
    private static final Comparator<EdgeId> COMPARATOR =
        Comparator.comparing(EdgeId::tail).thenComparing(EdgeId::head);

    public EdgeId(BgpPeerConfigId tail, BgpPeerConfigId head) {
      _tail = tail;
      _head = head;
    }

    public BgpPeerConfigId tail() {
      return _tail;
    }

    public BgpPeerConfigId head() {
      return _head;
    }

    public EdgeId reverse() {
      return new EdgeId(_head, _tail);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof EdgeId)) {
        return false;
      }
      EdgeId other = (EdgeId) o;
      return _tail.equals(other._tail) && _head.equals(other._head);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_tail, _head);
    }

    @Override
    public int compareTo(EdgeId o) {
      return COMPARATOR.compare(this, o);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("tail", _tail).add("head", _head).toString();
    }
  }
}
