package org.batfish.diagnosis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.security.jarsigner.JarSigner;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.Prefix;


import com.google.common.collect.Sets;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.dataplane.ibdp.Node;

/**
 * The forwarding tree is for specific single prefix (one dst, multi src)
 */

public class BgpForwardingTree {

    BgpTopology _bgpTopology;

    private String _vpnName;
    private boolean _ifMpls;
    // Destination (origin) router and the prefix in it.
    private Prefix _dstPrefix;
    private String _dstDevName;


    // The next-hop map is for each router to forward traffic
    // 只是针对【路由级的】每个节点【对应协议】计算出的下一跳的map
    // (It is allowed that each node reach its next-hop router using: 1) IGP, 2) directed connection, 3) Static)
    private Map<BgpPeerConfigId, BgpPeerConfigId> _nextHopForwardingMap;
    // The best route source map is for each router to receive and select its best route
    // 【路由级】路由传播和优选的from节点
    private Map<BgpPeerConfigId, BgpPeerConfigId> _bestRouteFromMap;
    // 一开始没有bgp路由的节点
    private Set<String> _unreachableNodes;
    private Set<BgpPeerConfigId> _reachableNodes;
    // 一开始接收的可达路由的集合, 最优的, 最长前缀匹配的
//    private Map<String, BgpRoute> _bestRouteMap;



    public enum PathType{
        BEST_ROUTE_FROM,
        BEST_FORWARDING
    }


    public String chooseFirstNodeHasUnreachablePeer(BgpTopology bgpTopology) {
        Set<BgpPeerConfigId> reachNodes = getReachableNodes();
        for (BgpPeerConfigId node : reachNodes) {
            bgpTopology.getEdges().forEach(edge->{
            });

        }
        assert reachNodes.size() < 1;
        return reachNodes.iterator().next().getHostname();
    }

    public BgpForwardingTree(String dstDev, Prefix prefix, String startDev, String vpnName, BgpTopology bgpTopology, Set<Node> nodes) {
        _dstDevName = dstDev;
        _dstPrefix = prefix;
        _nextHopForwardingMap = new HashMap<>();
        _bestRouteFromMap = new HashMap<>();
        _vpnName = vpnName;
//        _bestRouteMap = new HashMap<>();
//        _ifMpls = !vpnName.equals(KeyWord.PUBLIC_VPN_NAME);
        _bgpTopology = bgpTopology;
        // 初始时unreachNodes是除了dst外所有节点
        _unreachableNodes = new HashSet<>();
        nodes.forEach(node->{
            _unreachableNodes.add(node.getConfiguration().getHostname());
        });
        _unreachableNodes.remove(dstDev);
    }



    public String getDstDevName() {
        return _dstDevName;
    }


    public void addNextHopForwardingEdge(String head, String tail) {
        Logger logger = Logger.getLogger(KeyWord.LOGGER_NAME);
        if (_nextHopForwardingMap.containsKey(head)) {
            if (!_nextHopForwardingMap.get(head).equals(tail)) {
                logger.warning("INCONSISTENT FORWARDING BEHAVIOUR!!");
            }
        }
        _nextHopForwardingMap.put(head, tail);
    }

    public void addBestRouteFromEdge(String head, String tail) {
        Logger logger = Logger.getLogger(KeyWord.LOGGER_NAME);
        if (_bestRouteFromMap.containsKey(head)) {
            if (!_bestRouteFromMap.get(head).equals(tail)) {
                logger.warning("INCONSISTENT FORWARDING BEHAVIOUR!!");
            }
        }
        _bestRouteFromMap.put(head, tail);
    }

    // RECONSTRUCTION: genNewTree
    public void copyBestRouteFromMap(Map<String, String> map) {
        assert _bestRouteFromMap.size()<1;
        for (String node : map.keySet()) {
            _bestRouteFromMap.put(node, map.get(node));
        }
    }


    public Set<BgpPeerConfigId> getReachableNodes() {
        // TODO: 选哪个map作为参考？
        return _nextHopForwardingMap.keySet();
    }

    public Set<String> getUnreachableNodes() {
        return _unreachableNodesPrev;
    }

    // RECONSTRUCTION: genNewTree
    public boolean addBestRouteFromPath(List<String> path) {
        for (int i=0; i<path.size()-1; i+=1) {
            if (!_bestRouteFromMap.containsKey(path.get(i))) {
                _bestRouteFromMap.put(path.get(i), path.get(i+1));
            } else {
                if (!_bestRouteFromMap.get(path.get(i)).equals(path.get(i+1))) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<String> getBestRouteFromPath(String startDevName, String endDevName) {
        // start&end node are both in the returned path
        List<String> path = new ArrayList<>();
        path.add(startDevName);
        while(true) {
            String thisNode = path.get(path.size()-1);
            if (thisNode.equals(endDevName) || !_bestRouteFromMap.containsKey(thisNode) || thisNode.equals(_dstDevName)) {
                break;
            }
            String nextNode = _bestRouteFromMap.get(thisNode);

            if (!thisNode.equals(endDevName) && path.contains(nextNode)) {
                if (Main.printLog) {
                    System.out.println(path);
                    System.out.println("LOOP!!!!!!!!!!!");
                }

                return null;
            }
            path.add(nextNode);

        }
        // must be a valid path
        if (path.get(path.size()-1).equals(endDevName)) {
            return path;
        } else {
            return null;
        }
    }

    public List<String> getForwardingPath(String startDevName, String endDevName) {
        // start&end node are both in the returned path
        List<String> path = new ArrayList<>();
        path.add(startDevName);
        while(true) {
            String thisNode = path.get(path.size()-1);

            if (thisNode.equals(endDevName) || !_nextHopForwardingMap.containsKey(thisNode)) {
                break;
            }

            String nextNode = _nextHopForwardingMap.get(thisNode);
            if (!thisNode.equals(endDevName) && path.contains(nextNode)) {
                if (Main.printLog) {
                    System.out.println(path);
                    System.out.println("LOOP!!!!!!!!!!!");
                }

                return null;
            }
            path.add(nextNode);
        }
        // valid path
        if (path.get(path.size()-1).equals(endDevName)) {
            return path;
        } else {
            return null;
        }
    }


    public Map<String, Set<String>> getAllInNeighbors(Map<String, String> forwardingMap, Set<String> considerNodes) {
        // 入参是BGPTree的bestRouteFromMap
        Map<String, Set<String>> maps = new HashMap<>();
        for (String node : forwardingMap.keySet()) {
            // the value node is the out neighbor of the key node
            if (node.equals(forwardingMap.get(node))) {
                // dst节点的下一跳是自己，所以不把自己加入prop/in 集合中
                continue;
            }
            if (!maps.containsKey(forwardingMap.get(node))) {
                Set<String> nodes = new HashSet<>();
                if (considerNodes.contains(node) && considerNodes.contains(forwardingMap.get(node))) {
                    nodes.add(node);
                }

                maps.put(forwardingMap.get(node), nodes);
            } else {
                if (considerNodes.contains(node) && considerNodes.contains(forwardingMap.get(node))) {
                    maps.get(forwardingMap.get(node)).add(node);
                }

            }
        }
        return maps;
    }

    public Map<String, Set<String>> getAllOutNeighbors(Map<String, String> forwardingMap, Set<String> considerNodes) {
        Map<String, Set<String>> maps = new HashMap<>();
        for (String node : forwardingMap.keySet()) {
            // the value node is the out neighbor of the key node
            if (node.equals(forwardingMap.get(node))) {
                // dst节点的下一跳是自己，所以不把自己加入acpt/out 集合中
                continue;
            }
            if (!maps.containsKey(node)) {
                Set<String> nodes = new HashSet<>();
                if (considerNodes.contains(node) && considerNodes.contains(forwardingMap.get(node))) {
                    nodes.add(forwardingMap.get(node));
                }

                maps.put(node, nodes);
            } else {
                if (considerNodes.contains(node) && considerNodes.contains(forwardingMap.get(node))) {
                    maps.get(node).add(forwardingMap.get(node));
                }

            }
        }
        return maps;
    }

    public List<Long> getAsPath(String node, BgpTopology bgpTopology) {
        long thisAs = bgpTopology.getAsNumber(node);
        Set<Long> asPath = new LinkedHashSet<>();
        for (String other : getBestRouteFromPath(node, _dstDevName)) {
            long otherAs = bgpTopology.getAsNumber(other);
            if (otherAs==thisAs) {
                continue;
            }
            asPath.add(otherAs);
        }
        List<Long> asPathList = new ArrayList<>(asPath);
        Collections.reverse(asPathList);
        return asPathList;
    }

    public Map<String, Set<String>> getRRClients(Map<String, Set<String>> inNodes, Map<String, Set<String>> outNodes, BgpTopology bgpTopology) {
        // TODO 考虑和原有改动最小
        Map<String, Set<String>> clientsMap = new HashMap<>();
        for (String node : inNodes.keySet()) {
            Set<String> clients = new HashSet<>();
            // in-nodes是要传出去路由的节点
            Set<String> nodesIn = inNodes.get(node);
            // out-nodes是转发流量的下一跳，也是要优选的节点
            Set<String> nodesOut = outNodes.get(node);
            if (nodesOut==null) {
                // dstDev没有out nodes
                continue;
            }
            // 把已经配了的删除了
            List<String> nodesOutNotClient = bgpTopology.getNodesInAs(bgpTopology.getAsNumber(node), nodesIn).stream()
                    .filter(nodeIn->!bgpTopology.ifConfiguredRRClient(node, nodeIn)).collect(Collectors.toList());
            List<String> nodesInNotClient = bgpTopology.getNodesInAs(bgpTopology.getAsNumber(node), nodesIn).stream()
                    .filter(nodeIn->!bgpTopology.ifConfiguredRRClient(node, nodeIn)).collect(Collectors.toList());

            if (nodesInNotClient.size()==0 || nodesOutNotClient.size()==0) {
                continue;
            } else if (nodesInNotClient.size() <= nodesOutNotClient.size()) {
                nodesInNotClient.forEach(n->clients.add(n));
            } else {
                nodesOutNotClient.forEach(n->clients.add(n));
            }

            if (clients.size() > 0) {
                clientsMap.put(node, clients);
            }

        }
        return clientsMap;
    }

    public List<String> getNextHopList(String node, BgpTopology bgpTopology) {
        List<String> path = getBestRouteFromPath(node, _dstDevName);
        List<String> ipList = new ArrayList<>();
        for (String nextNode : path) {
            if (nextNode.equals(node)) {
                continue;
            }
            if (bgpTopology.getAsNumber(node)==bgpTopology.getAsNumber(nextNode)) {
                ipList.add(bgpTopology.getNodeIp(nextNode));
            }
        }
        if (ipList.size()>0) {
            return ipList;
        } else {
            return null;
        }

    }


    public Map<String, BgpCondition> genBgpConditions(Set<String> reqSrcNodes, BgpTopology bgpTopology) {
        Set<String> nodesSetCondition = _bestRouteFromMap.keySet();
        if (_ifMpls) {
            nodesSetCondition = new HashSet<>();
            for (String node: reqSrcNodes) {
                List<String> path = getBestRouteFromPath(node, _dstDevName);
                nodesSetCondition.addAll(path);
            }
        }
        // TODO: 1. assign the nextHop attribute for each route based on the traffic forwarding tree
        Map<String, BgpCondition> conds = new HashMap<>();
        // prop是in-nodes
        Map<String, Set<String>> propNeighborMap = getAllInNeighbors(_bestRouteFromMap, nodesSetCondition);
        // accept是out-nodes
        Map<String, Set<String>> acptNeighborMap = getAllOutNeighbors(_bestRouteFromMap, nodesSetCondition);

        Map<String, Set<String>> clientsMap = getRRClients(propNeighborMap, acptNeighborMap, bgpTopology);


        for (String node : nodesSetCondition) {
            // dst节点单独设置
            if (node.equals(_dstDevName)) {
                continue;
            }
            String vpnName = _vpnName;
            if (!ConfigTaint.getVpnInstance(node, vpnName).isValid()) {
                vpnName = KeyWord.PUBLIC_VPN_NAME;
            }
            // prop-neighbor和acpt-neighbor还有peer的list可以去除不在nodesSetCondition里的节点
            conds.put(node, new BgpCondition.Builder(_dstPrefix.toString())
                    // vpn名称只在有vpnName的节点上设置，其余设置public
                    .vpnName(vpnName)
                    .propNeighbors(propNeighborMap.get(node))
                    .acptNeighbors(acptNeighborMap.get(node))
                    .ibgpPeers(getIntersectantSet(bgpTopology.getBgpPeers(node, propNeighborMap.get(node), BgpPeerType.IBGP), nodesSetCondition))
                    .ebgpPeers(getIntersectantSet(bgpTopology.getBgpPeers(node, propNeighborMap.get(node), BgpPeerType.EBGP), nodesSetCondition))
                    .rrClient(clientsMap.get(node))
                    .selectionRoute(new SelectionRoute.Builder(_dstPrefix.toString())
                            .vpnName(vpnName)
                            .nextHop(getNextHopList(node, bgpTopology))
                            .asPath(getAsPath(node, bgpTopology))
                            .build())
                    .redistribution(false)
                    .build());
        }
        // 添加终点（原发）节点的redistribute约束
        String node = _dstDevName;
        conds.put(node, new BgpCondition.Builder(_dstPrefix.toString())
                .vpnName(_vpnName)
                .propNeighbors(propNeighborMap.get(node))
                .acptNeighbors(acptNeighborMap.get(node))
                .ibgpPeers(getIntersectantSet(bgpTopology.getBgpPeers(node, propNeighborMap.get(node), BgpPeerType.IBGP), nodesSetCondition))
                .ebgpPeers(getIntersectantSet(bgpTopology.getBgpPeers(node, propNeighborMap.get(node), BgpPeerType.EBGP), nodesSetCondition))
                .rrClient(clientsMap.get(node))
                .redistribution(true)
                .build());
        return conds;

    }

    /*
     * 求set1和set2的交集
     * */
    private Set<String> getIntersectantSet(Set<String> set1, Set<String> set2) {
        if (set1 != null) {
            set1.retainAll(set2);
        }

        return set1;
    }

    public void serializeBgpCondition(String filePath, Map<String, BgpCondition> conditions) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        String jsonString = gson.toJson(conditions);
        // System.out.println(jsonString);
        try{
            File file = new File(filePath);

            if(!file.getParentFile().exists()){
                //若父目录不存在则创建父目录
                file.getParentFile().mkdirs();
            }

            if(file.exists()){
                //若文件存在，则删除旧文件
                file.delete();
            }

            file.createNewFile();

            //将格式化后的字符串写入文件
            // FileWriter writer = new FileWriter(filePath);
            // writer.write(jsonString);
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write(jsonString);
            writer.flush();
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public boolean ifAllConnected() {
        // 检测这个树是否不连通
        // 基本思路：【forwardingMap】的keySet里的节点都有valid路径
        // TODO: 该检查哪个map？
        Set<String> nodesInTree = new HashSet<>(_nextHopForwardingMap.keySet());
        for (String node : _nextHopForwardingMap.keySet()) {
            List<String> path = getForwardingPath(node, _dstDevName);
            if (path==null) {
                continue;
            }
            if (nodesInTree.size()==0) {
                break;
            }
            nodesInTree.removeAll(path);
        }
        return nodesInTree.size()==0;
    }

    public String getBestNextHop(String node) {
        if (_nextHopForwardingMap.containsKey(node)) {
            return _nextHopForwardingMap.get(node);
        }
        return "";
    }

    public String getBestRouteFromNode(String node) {
        if (_bestRouteFromMap.containsKey(node)) {
            return _bestRouteFromMap.get(node);
        }
        return "";
    }

    public <T> boolean ifSetContainsSameElement(Set<T> inputSet, T eleT) {
        for (T t : inputSet) {
            if (t.equals(eleT)) {
                return true;
            }
        }
        return false;
    }

    /*
     * 计算满足转发可达所需的IGP互通的点对集合：
     * 1）IBGP Peer（只考虑原来在unreachableNodes里的，不在的说明已经可达了）
     * 2）所有节点BGP路由下一跳
     */
    /**
     * @param bgpTopology
     * @return
     */
    public Map<String, Set<Node>> computeReachIgpNodes(BgpTopology bgpTopology) {
        //
        Map<String, Set<Node>> reachNodes = new HashMap<>();
        Map<String, Set<String>> inNeighborMap = getAllInNeighbors(_bestRouteFromMap, _bestRouteFromMap.keySet());
        // Ibgp peer 互达
        for (String node : inNeighborMap.keySet()) {
            // 每个node都要和它的所有in节点建立peer关系（双向可达）
            if (!reachNodes.containsKey(node)) {
                reachNodes.put(node, new HashSet<Node>());
            }
            inNeighborMap.get(node).forEach(peer->{

                // node到peer可达
                String peerIp;
                if (bgpTopology.getNodeIp(peer)!=null) {
                    peerIp = bgpTopology.getNodeIp(peer);
                } else {
                    peerIp = _bestRouteMap.get(peer).getPeerIpString();
                }
                Node reachPeerNode = new Node(peer, peerIp);
                if (!ifSetContainsSameElement(reachNodes.get(node), reachPeerNode)) {
                    reachNodes.get(node).add(reachPeerNode);
                }

                // peer到node也可达
                if (!reachNodes.containsKey(peer)) {
                    reachNodes.put(peer, new HashSet<Node>());
                }
                String thisIp;
                if (bgpTopology.getNodeIp(node)!=null) {
                    thisIp = bgpTopology.getNodeIp(node);
                } else {
                    thisIp = _bestRouteMap.get(node).getPeerIpString();
                }
                Node reachThisNode = new Node(node, thisIp);
                if (!ifSetContainsSameElement(reachNodes.get(peer), reachThisNode)) {
                    reachNodes.get(peer).add(reachThisNode);
                }
            });
        }
        // best-route下一跳可达
        if (_bestRouteMap!=null) {
            _bestRouteMap.forEach((node, route)->{
                if (!reachNodes.containsKey(node)) {
                    reachNodes.put(node, new HashSet<Node>());
                }
                // TODO 避开EBGP下一跳
                if (bgpTopology.getAsNumber(node)==bgpTopology.getAsNumber(route.getNextHopDev())) {
                    Node nextHopNode = new Node(route.getNextHopDev(), route.getNextHopIpString());
                    if (!ifSetContainsSameElement(reachNodes.get(node), nextHopNode)) {
                        reachNodes.get(node).add(nextHopNode);
                    }
                }
            });
        }

        return reachNodes;
    }

}

