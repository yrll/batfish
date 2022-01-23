package org.batfish.log;

//import java.util.ArrayList;
//import com.alibaba.fastjson.annotation.JSONField;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.dataplane.rib.RouteAdvertisement;

public class BgpLog implements Serializable {
//  @JSONField(name = "CAUSE", ordinal = 1)
//  public BgpCause cause;
//  @JSONField(name = "ACTION", ordinal = 2)
//  public BgpAction action;
//  @JSONField(name = "INFO", ordinal = 3)
//  public String info;
//
//  public String getInfo() {
//    return info;
//  }
//
//  public BgpAction getAction() {
//    return action;
//  }
//
//  public BgpCause getCause() {
//    return cause;
//  }
//
//  public BgpLog(String info){
//    this.info = info;
//  }
//
//  public BgpLog(BgpCause cause, BgpAction action){
//    //cause_action.put(cause, action);
//    this.cause = cause;
//    this.action = action;
//    //System.out.println("Cause:" + cause.type + "***" + "Action: "+ action.actionType);
//
//  }
//
//  public BgpLog(BgpCause cause){
//    this.cause = cause;
//  }
//
//  public BgpLog(BgpAction action){
//    this.action = action;
//  }
//
//  public void setAction(BgpAction action) {
//    this.action = action;
//  }
//
//  public void setCause(BgpCause cause) {
//    this.cause = cause;
//  }
//
//  public void ChangeLog(BgpCause cause){
//    //cause_action.remove(cause);
//    //System.out.println("remove cause from "+cause.sender);
//  }
//
//  public void tofile(Path path){
//    //JSONObject json = new JSONObject(cause_action);
//  }
//
//  public void changeRouteToString(){
//    this.cause.routeInfo = this.cause.getCauseRoute().toString();
//    this.action.routeInfo = this.action.getRoute().toString();
//
//  }
//  public Map<String, ArrayList<RouteAdvertisement<Bgpv4Route>>> _causeRoutes;
//
//  public ArrayList<AbstractRoute> _discardRoutes;
//
//  public ArrayList<AbstractRoute> _selectedRoutes;
//
//  public Map<AbstractRoute, ArrayList<String>> _announcedRoutes;
//
//  public BgpLog(){
//    _causeRoutes = new HashMap<String, ArrayList<RouteAdvertisement<Bgpv4Route>>>();
//    _discardRoutes = new ArrayList<AbstractRoute>();
//    _selectedRoutes = new ArrayList<AbstractRoute>();
//    _announcedRoutes = new HashMap<AbstractRoute, ArrayList<String>>();
//
//  }
  // who send the route
//  public static final String SENDER = "sender";
//  public static final String RECEIVED_ROUTE = "receivedRoute";
//  public static final String IS_ORIGIN = "isOrigin";
//  public static final String ACTION_TYPE = "actionType";
//  public static final String TRANSFORMED_ROUTE = "transformedRoute";
//  public static final String INSTALLED_ROUTE = "installedRoute";
//  public static final String SEND_ROUTE = "sendRoute";
//  public static final String RECEIVERS = "receivers";
//
//  public String _Sender;
//  // received route from neighbor, origin-route is null
//  public RouteAdvertisement<Bgpv4Route> _receivedRoute;
//  // local/connected/static route
//  public boolean _isOrigin;
//  // after received a route, the router's action
//  public ActionType _actionType;
//  // transformed route from received route, like, reedit receivedFromIp or some other attributes
//  public AbstractRoute _transformedRoute;
//  // route installed to mainRib
//  public AbstractRoute _installedRoute;
//  // no use now, redundant
//  public AbstractRoute _sendRoute;
//  // list of receivers of the route
//  public ArrayList<String> _rcvrs;
//
//  public long startTime;
//  public long endTime;
//
//  public BgpLog(){
//    this._isOrigin = false;
//    _rcvrs = new ArrayList<String>();
//  }
//
//  public void set_isOrigin(boolean _isOrigin) {
//    this._isOrigin = _isOrigin;
//  }
//
//  public void set_installedRoute(AbstractRoute _installedRoute) {
//    this._installedRoute = _installedRoute;
//  }
//
//  public void set_rcvrs(ArrayList<String> _rcvrs) {
//    this._rcvrs = _rcvrs;
//  }
//
//  public void add_rcvrs(String rcvr){
//    _rcvrs.add(rcvr);
//  }
//
//  public void set_receivedRoute(RouteAdvertisement<Bgpv4Route> _receivedRoute) {
//    this._receivedRoute = _receivedRoute;
//  }
//
//  public void set_Sender(String _Sender) {
//    this._Sender = _Sender;
//  }
//
//  public void set_sendRoute(AbstractRoute _sendRoute) {
//    this._sendRoute = _sendRoute;
//  }
//  public void set_actionType(ActionType _actionType) {
//    this._actionType = _actionType;
//  }
//
//  public void set_transformedRoute(AbstractRoute _transformedRoute) {
//    this._transformedRoute = _transformedRoute;
//  }
//
//  @JsonProperty(SENDER)
//  public String get_Sender() {
//    return _Sender;
//  }
//
//  @JsonProperty(RECEIVED_ROUTE)
//  public RouteAdvertisement<Bgpv4Route> get_receivedRoute() {
//    return _receivedRoute;
//  }
//
//  @JsonProperty(ACTION_TYPE)
//  public ActionType get_actionType() {
//    return _actionType;
//  }
//
//  @JsonProperty(TRANSFORMED_ROUTE)
//  public AbstractRoute get_transformedRoute() {
//    return _transformedRoute;
//  }
//
//  @JsonProperty(INSTALLED_ROUTE)
//  public AbstractRoute get_installedRoute() {
//    return _installedRoute;
//  }
//
//  @JsonIgnore
//  public AbstractRoute get_sendRoute() {
//    return _sendRoute;
//  }
//
//  @JsonProperty(RECEIVERS)
//  public ArrayList<String> get_rcvrs() {
//    return _rcvrs;
//  }
//
//  public boolean checkLogCompleteness(){
//    switch (_actionType){
//    case ERROR:{
//      System.out.println("ERROR_LOG: actiontype-ERROR");
//      return false;
//    }
//    case DISCARD:{
//      if ((_transformedRoute==null)&&(_installedRoute==null)){
//        return true;
//      }
//      System.out.println("ERROR_LOG: actiontype-DISCARD");
//      return false;
//    }
//    case RECEIVED :
//    case BGP_SELECTED: {
//      if ((_transformedRoute!=null)&&(_installedRoute==null)){
//        return true;
//      }
//      System.out.println("ERROR_LOG: actiontype-RECEIVED");
//      return false;
//    }
//    case INSTALL_TO_MAINRIB:{
//      return (_installedRoute!=null);
//    }
//    }
//    return false;
//  }
//
//  public void tranceReceivedRoute(BgpLogNode currentNode, BgpLog currentLog, Map<String, BgpLogs> nodes){
////      BgpLogNode currentNode = new BgpLogNode(selfHostName, _receivedRoute.getRoute());
//    if ((currentLog.get_Sender()==null)||(currentLog._isOrigin)){
//      return;
//    }
//    String parentHostName = currentLog.get_Sender();
//    for (BgpLog parentlog:nodes.get(parentHostName).logs){
//      if (parentlog._rcvrs.contains(currentNode.hostName)){
//        if (currentLog._receivedRoute.getRoute().equals(parentlog._sendRoute)){
//          if (parentlog._isOrigin){
//            BgpLogNode parentNode = new BgpLogNode(parentHostName, parentlog._installedRoute);
//            currentNode.setParent(parentNode);
//            parentNode.addKid(currentNode);
//            return;
//          }
//          BgpLogNode parentNode = new BgpLogNode(parentHostName, parentlog._receivedRoute.getRoute());
//          currentNode.setParent(parentNode);
//          parentNode.addKid(currentNode);
//          tranceReceivedRoute(parentNode, parentlog, nodes);
//        }
//      }
//
//    }
//  }
//
//  public BgpLogNode trace(String selfHostName, Map<String, BgpLogs> nodes){
//    BgpLogNode currentNode = new BgpLogNode(selfHostName);
//    if (_isOrigin){
//      currentNode.setRoute(_installedRoute);
//      traceSendRoute(currentNode, this, nodes);
//    }else {
//      currentNode.setRoute(_receivedRoute.getRoute());
//      tranceReceivedRoute(currentNode, this, nodes);
//      traceSendRoute(currentNode, this, nodes);
//    }
//    return currentNode;
//  }
//  public void traceSendRoute(BgpLogNode currentNode, BgpLog currentLog, Map<String, BgpLogs> nodes){
//    if (currentLog._rcvrs==null){
//      return;
//    }
//    for (String rcvr: currentLog._rcvrs){
//      BgpLogs kidLogs = nodes.get(rcvr);
//      for (BgpLog kidlog:kidLogs.getLogs()){
//        if (currentNode.hostName.equals(kidlog.get_Sender())){
//          if (currentLog._sendRoute.equals(kidlog._receivedRoute.getRoute())){
//            BgpLogNode kidNode = new BgpLogNode(rcvr, kidlog._receivedRoute.getRoute());
//            currentNode.addKid(kidNode);
//            kidNode.setParent(currentNode);
//            if (kidlog._rcvrs!=null){
//              traceSendRoute(kidNode, kidlog, nodes);
//            }
//            continue;
//          }
//        }
//      }
//    }
//  }
  public static final String PROP_ITER = "iter";
  public static final String PROP_CAUSE = "cause";
  public static final String PROP_RECEIVED = "bgpReceived";
  public static final String PROP_INSTALLED = "installed";

  public int _iter; //actually, a global variable
  private Map<String, ArrayList<RouteAdvertisement<Bgpv4Route>>> _cause;
  private Map<String, ArrayList<AbstractRoute>> _bgpReceived; //import policy
  private ArrayList<AbstractRoute> _installed;

  public BgpLog(){}
  public BgpLog(int iter){
    this._iter = iter;
    _cause = new HashMap<String, ArrayList<RouteAdvertisement<Bgpv4Route>>>();
    _bgpReceived = new HashMap<String, ArrayList<AbstractRoute>>();
    _installed = new ArrayList<AbstractRoute>();
  }

  @JsonProperty(PROP_ITER)
  public int get_iter(){return _iter;}
  @JsonProperty(PROP_CAUSE)
  public Map<String, ArrayList<RouteAdvertisement<Bgpv4Route>>> get_cause(){
    return _cause;
  }
  @JsonProperty(PROP_RECEIVED)
  public Map<String, ArrayList<AbstractRoute>> get_bgpReceived(){return _bgpReceived;}
  @JsonProperty(PROP_INSTALLED)
  public ArrayList<AbstractRoute> get_installed(){return _installed;};

  @JsonCreator
  public BgpLog(
      @Nullable @JsonProperty(PROP_ITER) int iter,
      @Nullable @JsonProperty(PROP_CAUSE) Map<String, ArrayList<RouteAdvertisement<Bgpv4Route>>> cause,
      @Nullable @JsonProperty(PROP_RECEIVED) Map<String, ArrayList<AbstractRoute>> received,
      @Nullable @JsonProperty(PROP_INSTALLED) ArrayList<AbstractRoute> installed
  ){
    _iter = iter;
    _cause = cause;
    _bgpReceived = received;
    _installed = installed;
  }

  public void addCause(String hostName, RouteAdvertisement<Bgpv4Route> causeRoute){
    if (_cause.get(hostName)==null){
      ArrayList<RouteAdvertisement<Bgpv4Route>> routes = new ArrayList<RouteAdvertisement<Bgpv4Route>>();
      routes.add(causeRoute);
      _cause.put(hostName, routes);
    } else {
      _cause.get(hostName).add(causeRoute);
    }
  }

  public void addReceived(String hostName, AbstractRoute route){
    if (_bgpReceived.get(hostName)==null){
      ArrayList<AbstractRoute> routes = new ArrayList<AbstractRoute>();
      routes.add(route);
      _bgpReceived.put(hostName, routes);
    } else {
      _bgpReceived.get(hostName).add(route);
    }
  }

  public void addInstalled(AbstractRoute route){
    _installed.add(route);
  }

  public void toFileSerializable(String path, String _hostName, boolean print){
    if ((this._cause.keySet().isEmpty())&(this._bgpReceived.keySet().isEmpty())){
      if (this._installed.isEmpty()){
        if (print){
          System.out.println("iter: "+_iter+"  router: "+_hostName+" ***no record***");
        }
        return;
      }

    }
    path = path + _hostName + "-bgplog"+_iter;
    try {
      ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path));
      os.writeObject(this);
      os.close();
      if (print){
        System.out.println("iter: "+_iter+"  router: "+_hostName);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
