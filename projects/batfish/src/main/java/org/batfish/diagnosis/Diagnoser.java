package org.batfish.diagnosis;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;


import org.batfish.datamodel.*;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.dataplane.ibdp.TopologyContext;
import org.batfish.dataplane.ibdp.VirtualRouter;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.common.DiagnosedFlow;
import org.batfish.diagnosis.conditions.BgpCondition;
import org.batfish.diagnosis.localization.*;
import org.batfish.diagnosis.reference.BgpForwardingTree;
import org.batfish.diagnosis.reference.BgpGenerator;
import org.batfish.diagnosis.util.ConfigTaint;

import static org.batfish.diagnosis.util.ConfigTaint.transPrefixOrIpToPrefixString;

/**
 * 针对一条flow的诊断,保存不同协议的诊断信息（当前主要是BGP的）
 * */
public class Diagnoser {
    private BgpGenerator bgpGenerator;
    private final DiagnosedFlow flow;

    DataPlane dataPlane;
    TopologyContext topologyContext;
    Map<String, Configuration> configurationMap;

    // 错误条件【外部输入】
    private Map<String, Violation> violations;

    public DiagnosedFlow getFlow() {
        return flow;
    }




    /**
     * 输入是一系列具有【相同dst节点&dst IP】的
     */
    public Diagnoser(DiagnosedFlow flow, DataPlane dataPlane,
                     TopologyContext topologyContext, Map<String, Configuration> configurationMap) {
        // input数据初始化
        this.flow = flow;
        // 输入1：配置根目录
        this.dataPlane = dataPlane;
        this.topologyContext = topologyContext;
        this.configurationMap = configurationMap;

        this.bgpGenerator = new BgpGenerator(flow, topologyContext.getBgpTopology(), topologyContext.getLayer3Topology());
        this.bgpGenerator.initializeBgpForwardingTree(dataPlane);
    }

    /*
     * 再serialize BGP之前，判断目的前缀是否在目的设备上源发了
     *      如果这里返回false，表示后面BGP的provenance文件失效（尽管文件里有prov信息，但是那是其他设备上源发的）
     * */

    public static String getTargetPrefixFromDirectOrStatic(String node, String curPrefixString, VirtualRouter virtualRouter) {
        Prefix curPrefix = null;
        if (Ip.tryParse(curPrefixString).isPresent()) {
            curPrefix = Ip.parse(curPrefixString).toPrefix();
        } else if (Prefix.tryParse(curPrefixString).isPresent()){
            curPrefix = Prefix.parse(curPrefixString);
        } else {
            assert false: "INPUT PREFIX_STRING INVALID: " + curPrefixString;
        }
        // STEP 1: 先在接口上遍历一遍查找
        Interface inf = LocalizationUtil.findTargetInfNameFromConfig(curPrefix, virtualRouter.getConfiguration());
        if (inf!=null) {
            return inf.getPrimaryNetwork().toString();
        }


        // STEP 2: 找配置里的static route
        for (StaticRoute r: virtualRouter.getConfiguration().getDefaultVrf().getStaticRoutes()) {
            if (r.getNetwork().containsPrefix(curPrefix)) {
                return r.getNetwork().toString();
            }
        }

        return curPrefix.toString();
    }


    public BgpGenerator getBgpGenerator() {
        return bgpGenerator;
    }

    public Map<String, BgpCondition> diagnose() {
        /*
           STEP1: BGP 诊断
            1) BGP 连通，诊断 BGP Peer
            2) BGP condition生成
         */

        // TODO: BGP Peer 生成

        Set<String> reachNodes = new HashSet<>();
        reachNodes.add(flow.getSrcNode());
        // use the correct traffic as a reference to generate the policy-compliant "Forwarding Tree"
        BgpForwardingTree reqTree;
        // 模块1：BGP路由可达诊断
        reqTree = bgpGenerator.genBgpTree(reachNodes, null);
        return reqTree.genBgpConditions(reachNodes, bgpGenerator.getBgpTopology());

    }

    public void printErrorLines(Map<String, Map<Integer, String>> lines) {
        for (String node: lines.keySet()) {
            System.out.println("---------------------------" + node + "---------------------------");
            List<Integer> lineNumList = new ArrayList<>(lines.get(node).keySet());
            Collections.sort(lineNumList);
            lineNumList.forEach(num->{
                System.out.println("[" + num + "]" + " " +lines.get(node).get(num));
            });

            System.out.println();
        }
    }


    public Map<String, Map<Integer, String>> mergeLinesMap(Map<String, Map<Integer, String>> map1, Map<String, Map<Integer, String>> map2) {
        for (String node: map2.keySet()) {
            if (map1.containsKey(node)) {
                map1.get(node).putAll(map2.get(node));
            } else {
                map1.put(node, map2.get(node));
            }
        }
        return map1;
    }

    //TODO: implementation
    private Set<String> getNodesInSameAs(String node, BgpTopology bgpTopology) {
        return new HashSet<String>();
    }

    /**
     * 最精确的检测方法是static的下一跳和BGP的转发路径不成环，但是由于无法提前得知BGP的实际转发路径，
     * 所以用了一个更强的条件：static下一跳和newBgpTree的下一跳一致
     * 输入:
     *  1) reqNodes是flow的requirement指定的src节点
     */
    public Map<String, Map<Integer, String>> localizeInconsistentStaticAndDirect(BgpGenerator newGenerator) {
        Map<String, Map<Integer, String>> lineMap = new HashMap<>();
        // 保持这个set是为了检查非隧道的节点时，避免一个AS内的节点反复查
        Set<String> nodesAlreadyChecked = new HashSet<>();
        Set<String> reqNodes = getNodesInSameAs(flow.getSrcNode(), bgpGenerator.getBgpTopology());

        for (String node : reqNodes) {
            if (node.equals(flow.getDstNode())) {
                continue;
            }

            if (checkNode.equals(flow.getDstNode())) {
                continue;
            }
            // bgp的next-hop应该是一个远端节点？邻接的下一跳节点还是需要迭代查IGP的路径？
            String bgpNextHop = newGenerator.getBgpTree().getBestNextHop(checkNode);
            // @TODO:获取节点的BGPRoute，后面用于设置是否需要考虑找到会影响转发的默认路由
            BgpRoute bgpRoute = newGenerator.getBgpTree().getBestBgpRoute(checkNode);
            if (newGenerator.getStaticTree().getNextHop(checkNode)!=null) {
                // 确定static tree上已经有静态路由的
                String staticNextHop = newGenerator.getStaticTree().getNextHop(checkNode);
                if (staticNextHop==null) {
                    // 表示这条静态路由的下一跳无效，不影响错误
                    continue;
                }
                // TODO 确定bestBgp来自EBGP/IBGP/LOCAL，比较其和Static的优先级
                if (bgpNextHop!=null && !staticNextHop.equals(bgpNextHop)) {
                    int staticPref = newGenerator.getStaticTree().getBestRoute(checkNode).getPref();
                    int bgpRoutePref = newGenerator.getBgpTree().getRouteTypePref(newGenerator.getBgpTopology().getNodesRelation(checkNode, bgpNextHop));
                    if (staticPref <= bgpRoutePref) {
                        StaticRoute route = newGenerator.getStaticTree().getBestRoute(checkNode);
                        Map<Integer, String> routeLines = ConfigTaint.staticRouteLinesFinder(checkNode, newGenerator.getStaticTree().getBestRoute(checkNode).getPrefix(), getConfigPath(checkNode));
                        route.printRoute();
                        lineMap.put(checkNode, routeLines);
                        addLocalizerToViolation(checkNode, new InconsistentRouteLocalizer(checkNode, route, routeLines));
                    }
                }
            } else {
                // 检查是否有direct路由
                Interface inf = newGenerator.getLayer2Topology().getIpLocatedInterface(checkNode, inputDstPrefixString);
                if (inf!=null) {
                    String infNextHop = newGenerator.getLayer2Topology().getPeerDevNameFromInface(checkNode, inf);
                    if (infNextHop!=null) {
                        if (infNextHop.equals(bgpNextHop)) {
                            continue;
                        }
                    }
                    Map<Integer, String> targetLines = ConfigTaint.interfaceLinesFinder(checkNode, inf.getInfName(), getConfigPath(checkNode));
                    addLocalizerToViolation(checkNode, new InconsistentRouteLocalizer(checkNode, inf.transToStaticRoute(), targetLines));
                    if (!lineMap.containsKey(checkNode)) {
                        // 打印错误路由
                        inf.transToStaticRoute().printRoute();
                        lineMap.put(checkNode, targetLines);
                    } else {
                        // 打印错误路由
                        inf.transToStaticRoute().printRoute();
                        lineMap.get(checkNode).putAll(targetLines);
                    }
                } else {
                    // 检查有没有dstPrefix包含的网段受到静态路由影响，这里输入的prefix是spec里指定的那个
                    Prefix inputPrefix = Prefix.parse(inputDstPrefixString);
                    boolean checkDefaultRoute = false;
                    if (bgpRoute!=null && bgpRoute.isDefaultIpv4Route()) {
                        checkDefaultRoute = true;
                    }
                    StaticRoute route = ConfigTaint.staticRouteFinder(checkNode, inputPrefix, false, checkDefaultRoute, getConfigPath(checkNode));
                    if (route!=null) {
                        // 加入lineMap
                        Map<Integer, String> routeLines = ConfigTaint.staticRouteLinesFinder(checkNode, route.getPrefix(), getConfigPath(checkNode));
                        addLocalizerToViolation(checkNode, new InconsistentRouteLocalizer(checkNode, route, routeLines));
                        if (!lineMap.containsKey(checkNode)) {
                            // 打印错误路由
                            route.printRoute();

                            lineMap.put(checkNode, routeLines);
                        } else {
                            // 打印错误路由
                            route.printRoute();
                            lineMap.get(checkNode).putAll(routeLines);
                        }
                    }
                }
            }


        }
        return lineMap;
    }

    public String getConfigPath(String node) {
        return flow.getConfigPath(node);
    }

    public static String fromJsonToString(String filePath) {
        File file = new File(filePath);
        String jsonStr = "";
        if(file.exists()){
            try {
                jsonStr = FileUtils.readFileToString(file,"UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsonStr;
    }

    /**
     * Localization入口
     * @param violationMap 第二次仿真的 path condition
     * @param newGenerator 第二次仿真的 信息
     * @return
     */
    public Map<String, List<ConfigurationLine>> localize(Map<String, Violation> violationMap, BgpGenerator newGenerator) {

        violations = violationMap;

        Map<String, Map<Integer, String>> errlines = new HashMap<>();

        // STEP 1: 根据violations定位BGP协议相关的配置错误行
        localizeErrorsFromViolations(newGenerator);

        // STEP 2: 根据BGP路由收敛的结果诊断静态路由的错误（还需完善，这个对静态路由的检查是互相依赖的，所以如果全部查到，应该要等BGP、IGP都收敛后才行）
        localizeInconsistentStaticAndDirect(newGenerator);

        // STEP 3: 根据BGP路由收敛的结果诊断BGP VPN上

        // STEP 4: 检查隧道使能

    }

    /**
     * @param
     * @return {@link Map}<{@link String}, {@link Map}<{@link Integer}, {@link String}>>
     * @FIXME 所有localizer都有记录，不需要在这里返回linesMap
     */

    public Map<String, Map<Integer, String>> localizeErrorsFromViolations(BgpGenerator newGenerator) {

        Map<String, Map<Integer, String>> errMap = new LinkedHashMap<>();
        // 输入是violated condition文件的路径
        if (violations != null && violations.size() > 0) {
            violations.forEach((node, vio) -> {
                getErrorLinesForSingleNode(node, newGenerator);
            });
            // 将每个violation的localizer的具体错误行放到返回的map里【peer要单独处理，可能peer上也有错误行】
            violations.forEach((node, vio) -> {
                vio.getLocalizers().forEach(localizer -> {
                    if (!errMap.containsKey(node)) {
                        errMap.put(node, new HashMap<>());
                    }
                    errMap.get(node).putAll(ConfigurationLine.transToMap(localizer.getErrorLines()));
                    if (localizer instanceof PeerLocalizer) {
                        if (((PeerLocalizer) localizer).getPeerErrorLines() != null) {
                            String peerNode = ((PeerLocalizer) localizer).getRemoteNode();
                            if (!errMap.containsKey(peerNode)) {
                                errMap.put(peerNode, new HashMap<>());
                            }
                            errMap.get(peerNode).putAll(((PeerLocalizer) localizer).getPeerErrorLines());
                        }
                    }
                });
            });
            return errMap.entrySet().stream().filter(m -> m.getValue() != null && m.getValue().size() > 0).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return errMap;
        }
    }

    private <T> boolean ifListValid(List<T> list) {
        return list!=null && list.size()>0;
    }

    public Map<Integer, String> getErrorLinesForSingleNode(String node, BgpGenerator newGenerator) {

        // 未配置反射客户的错误
        if (ifListValid(violatedRrClient)) {
            localizers.add(new ReflectClientLocalizer(curDevName, violatedRrClient, this, errGenerator.getBgpTopology(), configPath));
        }

        /*
         未能正常接收目标路由，有以下原因：
           1）策略拦截：如果importPolicy字段非空
           2）非策略拦截原因：
             * a）peer关系未建立
             * b）VPN交叉失败
             * c）迭代不到隧道
             *
             * 先按a）处理
        */
            if (ifListValid(violatedAcptNeighbors)) {
                violatedAcptNeighbors.forEach(n -> {
                    if (Prefix.parse(n.getIpPrefixString()).containsPrefix(errGenerator.getFlow().getDstPrefix())) {
                        if (n.getImRoutePolicyName()!=null) {
                            RouteForbiddenLocalizer routeForbiddenLocalizer = new RouteForbiddenLocalizer(curDevName, n,
                                    Direction.IN, this, configPath, newGenerator.getBgpTopology());
                            localizers.add(routeForbiddenLocalizer);
                        } else {
                            String peer = n.getSenderDevName(newGenerator.getBgpTopology());
                            if (peer != null) {
                                // 1) peer
                                if (!errGenerator.getBgpTopology().isValidPeer(curDevName, peer)) {
                                    // peer都是new两边，但是mei
                                    localizers.add(new PeerLocalizer(curDevName, peer, errGenerator, this,
                                            newGenerator.getBgpTopology()));
                                    // results.add();
                                }
                                String receiveNodeCfgPath = InputData.findFilePathWithMatchedName(errGenerator.getFlow().getCfgRootPath(), curDevName);
                                VpnInstance receiveVpnInstance = ConfigTaint.getVpnInstance(curDevName, vpnName, receiveNodeCfgPath);
                                String sendNodeCfgPath = InputData.findFilePathWithMatchedName(errGenerator.getFlow().getCfgRootPath(), peer);
                                VpnInstance sendVpnInstance = ConfigTaint.getVpnInstance(curDevName, vpnName, sendNodeCfgPath);
                                if (!receiveVpnInstance.canCrossFrom(sendVpnInstance)) {
                                    localizers.add(new VpnCrossLocalizer(sendVpnInstance, receiveVpnInstance, VpnInstance.fixedVpnTypeForNetwork(errGenerator.getFlow().getNetworkType())));
                                }
                            }

                        }
                    }

                });
            }

        /*
         未能正常发送目标路由，有以下原因：
           1）策略拦截：如果exportPolicy字段非空【还要再检查newGenerator里prop的节点是否已有默认路由，有默认路由就默认这个violatedProp是误报】
           2）非策略拦截原因：
             * a）peer关系未建立
             * c）迭代不到隧道
             *
        */
            if (ifListValid(violatedPropNeighbors)) {
                violatedPropNeighbors.forEach(n -> {
                    // 当前条目的prefix要包含dstPrefix才继续下一步
                    if (Prefix.parse(n.getIpPrefixString()).containsPrefix(errGenerator.getFlow().getDstPrefix())) {
                        if (n.getExRoutePolicyName()!=null) {
                            // 检测是否已有默认路由
                            Bgpv4Route bgpRouteInNewGenerator = newGenerator.getBgpTree().getBestBgpRoute(n.getToDeviceName());
                            if (bgpRouteInNewGenerator==null || !bgpRouteInNewGenerator.isDefaultIpv4Route()) {
                                RouteForbiddenLocalizer routeForbiddenLocalizer = new RouteForbiddenLocalizer(curDevName, n,
                                        Direction.OUT, this, configPath, newGenerator.getBgpTopology());
                                localizers.add(routeForbiddenLocalizer);
                            }
                        } else {
                            String peer = n.getFromDeviceName();
                            if (peer == null) {
                                peer = n.getSenderDevName(newGenerator.getBgpTopology());
                            }
                            if (peer != null) {
                                if (!errGenerator.getBgpTopology().isValidPeer(curDevName, peer)) {
                                    // peer都是new两边，但是mei
                                    localizers.add(new PeerLocalizer(curDevName, peer, errGenerator, this,
                                            newGenerator.getBgpTopology()));
                                }
                            }
                        }
                    }

                });
            }

            if (ifSetValid(violateEbgpPeer)) {
                violateEbgpPeer.forEach(n -> {
                    localizers.add(new PeerLocalizer(curDevName, n, errGenerator, this, errGenerator.getBgpTopology()));
                });
            }

            if (ifSetValid(violateIbgpPeer)) {
                violateIbgpPeer.forEach(n -> {
                    localizers.add(new PeerLocalizer(curDevName, n, errGenerator, this, errGenerator.getBgpTopology()));
                });
            }

            if (violateRedis != null && !violateRedis.equals("")) {
                StaticRoute targetRoute = originStaticRoute;
                if (originStaticRoute == null && originDirectRoute != null) {
                    targetRoute = originDirectRoute;
                } else {
                    /*
                     * 有redistribution的错，但是没有violatedRoute，生成一条valid的静态路由
                     *  1）先找有没有静态路由 或者 直连路由
                     */
                    String targetPrefixString = ipPrefixString;
                    Prefix targetPrefix = Prefix.parse(transPrefixOrIpToPrefixString(targetPrefixString));

                    // 扫描直连
                    Interface inf = errGenerator.getLayer2Topology().getIpLocatedInterface(curDevName, ipPrefixString);
                    if (inf!=null) {
                        originDirectRoute = inf.transToStaticRoute();
                    }

                    originStaticRoute = ConfigTaint.staticRouteFinder(curDevName, targetPrefix, true, false, configPath);
                    if (originDirectRoute!=null) {
                        targetRoute = originDirectRoute;
                    } else if (originStaticRoute!=null ){
                        targetRoute = originStaticRoute;
                    } else {
                        String nextHopString = errGenerator.getBgpTopology().getNodeIp(curDevName);
                        if (targetPrefix.getPrefixLength() == 32) {
                            // 目标IP是32位主机号， 换成一个随便的网段
                            targetPrefixString = targetPrefix.getStartIp().toString() + "/30";
                        }
                        targetRoute = new StaticRoute(curDevName, vpnName, targetPrefixString, nextHopString);
                    }

                }
                RedistributionLocalizer redistributionLocalizer = new RedistributionLocalizer(curDevName, violateRedis, targetRoute, this,
                        errGenerator.getLayer2Topology(), configPath);
                localizers.add(redistributionLocalizer);
                // 如果是由policy拦截的，还要再生成policy Forbidden的localizer
                if (redistributionLocalizer.getErrorTypes().contains(RedistributionLocalizer.RedisErrorType.POLICY)) {
                    localizers.add(new RouteForbiddenLocalizer(curDevName, targetRoute, redistributionLocalizer.getPolicyName(), this, configPath));
                }

            }

            Map<Integer, String> lineMap = new LinkedHashMap<>();
            localizers.forEach(n -> {
                lineMap.putAll(n.genErrorConfigLines());
            });
            return lineMap;
        }







    public void genIgpConstraints(BgpGenerator newGenerator, boolean ifSave) {}

    private void addLocalizerToViolation(String node, Localizer localizer) {
        if (localizer==null) {
            return;
        }
        if (!violations.containsKey(node)) {
            violations.put(node, new Violation());
        }
        violations.get(node).addResults(localizer);
    }


    /**
     * 【针对IPRAN】 IPMETRO不用mpls, CLOUDNET不用MPLS LDP
     * find if all vpn binding interface have configured mpls ldp, also global mpls ldp
     * STEP 1: 全局使能
     * FIXME mpls lsr-id错配/漏配未实现
     * STEP 2: 接口使能【检查与其他设备接口相连的那些，layer2Topo上的edge】
     * FIXME 什么样的接口才需要检查隧道使能？（IGP路径上的？）
     */

}
