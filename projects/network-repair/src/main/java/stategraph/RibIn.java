package stategraph;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.log.BgpLog;

public class RibIn implements Serializable {
  private Prefix prefix;
  private int time;
  private Map<String, AbstractRoute> rib;
  private AbstractRoute selectedRoute;
  private String selectedNeighbor;

  public RibIn() {
    this.time = time;
    this.rib = new HashMap<>();
  }

  public RibIn(int time, Prefix prefix){
    this.time = time;
    this.rib = new HashMap<>();
//    this.selectedRoute = null;
  }

  public RibIn(int time, Map<String, AbstractRoute> rib, AbstractRoute selectedRoute, String selectedNeighbor) {
    this.time = time;
    this.rib = rib;
    this.selectedRoute = selectedRoute;
    this.selectedNeighbor = selectedNeighbor;
  }

  public int getTime() {
    return time;
  }

  public AbstractRoute getSelectedRoute() {
    return selectedRoute;
  }

  public Map<String, AbstractRoute> getRib(){
    return rib;
  }

  public void setSelectedNeighbor(String selectedNeighbor) {
    this.selectedNeighbor = selectedNeighbor;
  }

  public void setSelectedRoute(AbstractRoute selectedRoute) {
    this.selectedRoute = selectedRoute;
  }

  public void setRib(Map<String, AbstractRoute> rib) {
    this.rib = rib;
  }

  public void setTime(int time) {
    this.time = time;
  }

  public void addRoute(String router, AbstractRoute route){
    this.rib.put(router, route);
  }

  public void mergeRibIn(Map<String, AbstractRoute> rib1) {
    if (rib1 == null) {
      return;
    }
    for (String neighbor : rib1.keySet()) {
      this.rib.put(neighbor, rib1.get(neighbor));
    }
  }

  public void updateSelectedRoute(BgpLog log, Prefix prefix) {
    if (getPrefixInstalledRoute(log, prefix) != null) {
      this.selectedRoute = getPrefixInstalledRoute(log, prefix);
      this.selectedNeighbor = getPrefixSelectedNeighbor(log, prefix);
    }
  }

  public AbstractRoute getPrefixInstalledRoute(BgpLog log, Prefix prefix) {
    for (AbstractRoute route: log.get_installed()) {
      if (route.getNetwork() == prefix) {
        return route;
      }
    }
    return null;
  }
  public Map<String, AbstractRoute> getPrefixCausedRouteMap(BgpLog log, Prefix prefix) {
    Map<String, AbstractRoute> ribIn = new HashMap<>();
    for (String neighbor: log.get_cause().keySet()) {
      for (RouteAdvertisement<Bgpv4Route> route: log.get_cause().get(neighbor)) {
        if (route.getRoute().getNetwork() == prefix) {
          ribIn.put(neighbor, route.getRoute());
          break;
        }
      }
    }
    return ribIn;
  }

  public String getPrefixSelectedNeighbor(BgpLog log, Prefix prefix) {
    for (AbstractRoute route: log.get_installed()) {
      if (route.getNetwork() == prefix) {
        for (String router : log.get_bgpReceived().keySet()) {
          for (AbstractRoute route1:log.get_bgpReceived().get(router)) {
            if (route.equals(route1)) {
              return router;
            }
          }
        }
      }
    }
    return null;
  }


//  protected Stack clone() throws CloneNotSupportedException {
//    Stack result = (Stack) super.clone();
//    result.elements = elements.clone(); //对elements元素进行拷贝（引用或基本数据类型）
//    return result;
//  }

}
