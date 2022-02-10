package constraints;

import java.util.ArrayList;

public class PathReq {

  public enum Protocols {
    BGP,
    OSPF,
    RIP
  }

  public Protocols protocol;
  public String dst_prefix;
  public ArrayList<String> paths;
  //  public boolean strict;

  public PathReq(Protocols protocol, String dst_prefix, ArrayList<String> paths) {
    this.protocol = protocol;
    this.dst_prefix = dst_prefix;
    this.paths = paths;
  }

  public boolean equals(PathReq pathReq) {
    if(this.protocol != pathReq.protocol)
      return false;
    if(!this.dst_prefix.equals(pathReq.dst_prefix))
      return false;
    return this.paths.equals(pathReq.paths);
  }
}
