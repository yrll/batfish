package org.batfish.diagnosis.localization;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.datamodel.*;
import org.batfish.diagnosis.reference.BgpGenerator;
import org.batfish.diagnosis.repair.Repairer;
import org.batfish.diagnosis.repair.RouteForbiddenRepairer;

import java.util.*;

import static org.batfish.diagnosis.util.ConfigTaint.transPrefixOrIpToPrefixString;

// 每个设备一个violation实例

public class Violation {
    @JsonProperty("ipPrefix")
    String ipPrefixString;
    String vpnName;
    List<String> violatedRrClient;
    List<Bgpv4Route> violatedPropNeighbors;
    List<Bgpv4Route> violatedAcptNeighbors;
    // prefer 的表示?
    List<Map<String, Bgpv4Route>> violatedRoutePrefer;
    Set<String> violateIbgpPeer;
    Set<String> violateEbgpPeer;
    StaticRoute originStaticRoute;
    ConnectedRoute originDirectRoute;
    // DirectRoute originDirectRoute;
    int missingLineCounter;

    // 描述redis失败的原因的字符串，用逗号分隔多个原因
    String violateRedis;
    // 自身节点的所有localizer
    Set<Localizer> localizers = new HashSet<>();

    public String getVpnName() {
        return vpnName;
    }

    /*
     * 对violation里的各种violated rules去重
     */
    public void preProcessOfViolation() {
        // STEP 1: 先处理重复的violatedPropNeighbors/violatedAcptNeighbors;
    }

    public void addViolateEbgpPeer(String node) {
        if (violateEbgpPeer == null) {
            violateEbgpPeer = new HashSet<String>();
        }
        violateEbgpPeer.add(node);
    }

    public void addViolateIbgpPeer(String node) {
        if (violateIbgpPeer == null) {
            violateIbgpPeer = new HashSet<String>();
        }
        violateIbgpPeer.add(node);
    }

    public static <T> boolean ifListValid(List<T> aimList) {
        if (aimList == null || aimList.size() < 1) {
            return false;
        }
        return true;
    }

    public static <T> boolean ifSetValid(Set<T> aimList) {
        if (aimList == null || aimList.size() < 1) {
            return false;
        }
        return true;
    }

    public int getMissingLine() {
        missingLineCounter -= 1;
        return missingLineCounter;
    }

    // public List<BgpRoute>

    public List<Bgpv4Route> getViolatedPropNeighbors() {
        return violatedPropNeighbors;
    }

    public List<Bgpv4Route> getViolatedAcptNeighbors() {
        return violatedAcptNeighbors;
    }

    public Set<String> getViolateEbgpPeers() {
        return violateEbgpPeer;
    }

    public Set<String> getViolateIbgpPeers() {
        return violateIbgpPeer;
    }

    public Set<Localizer> getLocalizers() {
        return localizers;
    }

    public void addResults(Localizer localizer) {
        localizers.add(localizer);
    }

    /*
     * 如果之前有repairer使用过同样的policy，则把相应的routePolicy返回，新的routeForbiddenRepairer引用它
     */
    private RouteForbiddenRepairer getRouteForbiddenRepairerWithSamePolicyName(String name, Set<Repairer> repairSet) {
        for (Repairer repairer: repairSet) {
            if (repairer instanceof RouteForbiddenRepairer) {
                if (((RouteForbiddenRepairer) repairer).getPolicyName().equals(name)) {
                    return (RouteForbiddenRepairer) repairer;
                }
            }
        }
        return null;
    }
    
    public Set<Repairer> tryRepair(String curNode) {
        Set<Repairer> repairSet = new HashSet<>();
        for (Localizer localizer: localizers) {
            // @TODO: routeForbidden的修复，对于同一个policy的多个localizer/repairer需引用同一个routePolicy实例【不需要是对同一个neighbor的】
            Repairer repairer = localizer.genRepairer();
            repairer.genRepair();
            repairSet.add(repairer);
        }
        return repairSet;
    }
}
