package stategraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.log.BgpLogs;
import org.junit.Test;

public class StateGraphTest {
  public Map<String, BgpLogs> getLogsMap(String path) {
    String networkName = "15nodes";
    //    networkName = path;
    //    String path1 = System.getProperty("user.dir")+"/log-serialize/"+networkName+"/";
    Map<String, BgpLogs> logsMap = new HashMap<String, BgpLogs>();
    File file = new File(path);
    File[] fileList = file.listFiles();
    for (int i=0; i< Objects.requireNonNull(fileList).length; i++){
      ObjectInputStream in = null;
      try {
        in = new ObjectInputStream(new FileInputStream(fileList[i].getAbsoluteFile()));
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        assert in != null;
        BgpLogs logs = (BgpLogs) in.readObject();
        logsMap.put(logs.get_hostName(),logs);
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
      System.out.println("finish: "+ i);
    }
    return logsMap;
  }

  public Prefix getArbitraryPrefix(BgpLogs logs) {
    for (int i = logs.get_logs().size()-1; i > 0; i--) {
      if (logs.get_logs().get(i).get_installed().size() != 0) {
        return logs.get_logs().get(i).get_installed().get(0).getNetwork();
      }
    }
    return Prefix.create(Ip.create(0), 32);
  }

  @Test public void constructStateGraph() {
    String networkName = "15nodes";
    String path = "/home/yrl/Desktop/batfish-2021-03-16-minesweeper/log-serialize/"+networkName+"/";
    Map<String, BgpLogs> logsMap = getLogsMap(path);
    BgpLogs logs = logsMap.get("as1border1");
    Prefix prefix = getArbitraryPrefix(logs);
    StateGraph stateGraph = new StateGraph(prefix);
    stateGraph.constructStateGraph(logsMap);
    System.out.println("~");
  }
}
