package routemap;

public class MatchSelectOne {
  public String[] match_types = new String[] {
      "MatchIpPrefix", "MatchNextHop",
      "MatchCommunity", "MatchAsPath" };

  public MatchIpPrefix mip;
  public MatchNextHop mnh;
  public MatchCommunity mc;
  public MatchAsPath map;

  public MatchSelectOne() {
    //      this.mip = new MatchIpPrefix();
    //      this.mnh = new MatchNextHop();
    //      this.mc = new MatchCommunity();
    //      this.map = new MatchAsPath();
  }

  public void generate_template(String hostname, String [] match_types, String network, String community){
    if (match_types == null || match_types.length == 0)
      match_types = this.match_types;
    for (int i=0; i<match_types.length; i++) {
      if (match_types[i].equals("MatchIpPrefix"))
        this.mip = new MatchIpPrefix(new IpPrefix(hostname, "permit", network));
      else if (match_types[i].equals("MatchNextHop"))
        this.mnh = new MatchNextHop(null);
      else if (match_types[i].equals("MatchCommunity"))
        this.mc = new MatchCommunity(new Community(hostname, "permit", community));
      else
        this.map = new MatchAsPath(null);
    }
  }

  static class MatchIpPrefix {

    public static String match_type = "MatchIpPrefix";
    public IpPrefix ip_pre;

    public MatchIpPrefix(IpPrefix ip_pre) {
      this.ip_pre = ip_pre;
    }

  }

  static class MatchNextHop {
    public static String match_type = "MatchNextHop";
    public String next_hop;

    public MatchNextHop(String next_hop) {
      this.next_hop = next_hop;
    }
  }

  static class MatchCommunity {
    public static String match_type = "MatchCommunity";
    public Community community;

    public MatchCommunity(Community community) {
      this.community = community;
    }

  }

  static class MatchAsPath {
    public static String match_type = "MatchAsPath";
    public String aspath;

    public MatchAsPath(String aspath) {
      this.aspath = aspath;
    }
  }
}
