package stategraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.log.BgpLog;
import org.batfish.log.BgpLogs;

public class State {
  private List<RibIn> ribs;

  private String router;

  private Prefix prefix;

//  public State() {}

  public State(String router, Prefix prefix) {
    this.router = router;
    this.prefix = prefix;
    this.ribs = new ArrayList<>();
  }

  public State(String router, Prefix prefix, List<RibIn> ribs) {
    this.ribs = ribs;
    this.prefix = prefix;
    this.router = router;
  }

  public void addNode(RibIn ribin) {
    this.ribs.add(ribin);
  }

  public String getRouter() {
    return router;
  }

  public RibIn getOneTimeNode(int time) {
    for (int i = ribs.size()-1; i > 0; i--){
      if (ribs.get(i).getTime() <= time) {
        return ribs.get(i);
      }
    }
    return null;
  }

  public List<RibIn> getBeforeTimeStates(int time) {
//    List<RibIn> ribs = new ArrayList<>();
    int index = 0;
    for (int i = this.ribs.size()-1; i > 0; i--){
      if (this.ribs.get(i).getTime() <= time) {
        index = i;
      }
    }
    return this.ribs.subList(0, index);
  }

  public RibIn getZeroState() {
    // get the initial state;
    return ribs.get(0);
  }

  public State constructState(BgpLogs logs) {
    String debugHost = "as1border1";
    for (int i = 0; i < logs.get_logs().size(); i++) {
      BgpLog log = logs.get_logs().get(i);
      if (i == 0) {
        // initial rib(stateNode), only connected/local route;
        RibIn initialRibIn = new RibIn(0, this.prefix);
        Map<String, AbstractRoute> routeMap = new HashMap<>();
        AbstractRoute connectedRoute =
            initialRibIn.getPrefixInstalledRoute(log, this.prefix);
        if (connectedRoute != null) {
          routeMap.put("connected", connectedRoute);
          initialRibIn.setRib(routeMap);
          initialRibIn.setSelectedRoute(connectedRoute);
          initialRibIn.setSelectedNeighbor("Origin");
        }
        ribs.add(0, initialRibIn);
        continue;
      }
//      new RibIn(logs.get_logs().get(i).get_iter(), this.prefix);
      RibIn ribIn;
      ribIn = CloneUtils.clone(ribs.get(ribs.size()-1));
      ribIn.setTime(logs.get_logs().get(i).get_iter());
//      ribIn = (RibIn) SerializationUtils.clone(ribs.get(ribs.size()-1));
      assert ribIn != null;
      ribIn.mergeRibIn(ribIn.getPrefixCausedRouteMap(log, this.prefix));
      ribIn.updateSelectedRoute(log, prefix);
      ribs.add(ribIn);
    }
    return this;
  }

}
