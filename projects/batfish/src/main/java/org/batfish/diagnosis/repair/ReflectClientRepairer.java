package org.batfish.diagnosis.repair;

import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.localization.ReflectClientLocalizer;
import org.batfish.diagnosis.util.ConfigTaint;
import org.batfish.diagnosis.util.KeyWord;


import java.util.List;
import java.util.Map;

public class ReflectClientRepairer extends Repairer {

    ReflectClientLocalizer localizer;

    // 这个错误已经确定了就是需要添加行
    public ReflectClientRepairer(ReflectClientLocalizer localizer) {
        List<ConfigurationLine> lines = localizer.genErrorConfigLines();
        String curNode = localizer.getLocalDevName();
        for (BgpPeerConfig bgpPeerConfig : localizer.getClients()) {
            String peerIp = bgpPeerConfig.getLocalIp().toString();
            String[] keyWords = {KeyWord.BGP_NEIGHBOR, peerIp };
            // TODO: 指定address-family下设置该命令
            ConfigurationLine configurationLine = ConfigTaint.findBgpAddressFamilyLine(localizer.getCfgPath());
            addAddedLine(configurationLine.getLineNumber(), ConfigTaint.genMissingBgpReflectLine(peerIp));
        }
    }

    @Override
    public void genRepair() {
        // TODO Auto-generated method stub
        return;
    }

}
