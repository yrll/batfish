package stategraph;

import java.util.HashMap;
import java.util.Map;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;
import org.batfish.log.BgpLogs;

public class StateGraph {
  private Prefix prefix;
  private Map<String, Configuration> _configs;
  private Map<String, State> graph ;

  public StateGraph(Prefix prefix) {
//    _configs = configs;
    this.prefix = prefix;
    this.graph = new HashMap<String,State>();
  }

  public StateGraph(Prefix prefix, Map<String, Configuration> configs) {
    this.prefix = prefix;
    this._configs = configs;
    this.graph = new HashMap<String,State>();
  }

  public Map<String, State> getGraph() {
    return graph;
  }

  public void setState(String router, State s) {
    graph.put(router, s);
  }

  public State getState(String router) {
    return graph.get(router);
  }

  public Map<String, RibIn> getOneTimeRibs(int time) {
    Map<String, RibIn> ribs = new HashMap<>();
    for (String r:graph.keySet()) {
      ribs.put(r, graph.get(r).getOneTimeNode(time));
    }
    return ribs;
  }

  /** get all state node map for all routers before the time t*/
  public Map<String, State> getSubStateGraph(int time) {
    Map<String, State> ribsMap = new HashMap<>();
    for (String r:graph.keySet()) {
      State state = new State(r, this.prefix, graph.get(r).getBeforeTimeStates(time));
      ribsMap.put(r, state);
    }
    return ribsMap;
  }

  public void constructStateGraph(Map<String, BgpLogs> logsMap) {
    for (String router: logsMap.keySet()) {
      State state = new State(router, prefix);
      graph.put(router, state.constructState(logsMap.get(router)));
    }

  }

  //  public List
}
