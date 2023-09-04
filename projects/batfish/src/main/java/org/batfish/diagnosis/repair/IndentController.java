package org.batfish.diagnosis.repair;

import java.util.HashSet;
import java.util.Set;

/**
 * 缩进控制器: 根据不同的语句添加缩进空格
 *
 * @author yrl
 * @date 2023/08/18
 */

public class IndentController {
    private static final Set<String> zeroIndentSet = new HashSet<>(){{
        add("route-map");
        add("ip route");
    }};
    private static final Set<String> oneIndentSet = new HashSet<>(){{
        add("match");
        add("vlan");
        add("shutdown");
        add("mpls");
    }};
    private static final Set<String> twoIndentSet = new HashSet<>(){{
        add("import");
        add("neighbor");
        add("network");
        add("vpn-target");
    }};

    public static String process(String line) {
        line = line.strip();
        for (String word: oneIndentSet) {
            if (line.contains(word)) {
                return " " + line;
            }
        }
        for (String word: twoIndentSet) {
            if (line.contains(word)) {
                return "  " + line;
            }
        }
        return line;
    }

}
