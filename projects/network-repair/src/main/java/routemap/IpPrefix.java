package routemap;

public class IpPrefix {
  public String hostname;
  public String access;
  public String networks;
  public Integer prefix;

  public IpPrefix(String hostname, String access, String networks) {
    this.hostname = hostname;
    this.access = access;
    this.networks = networks;
    //      this.prefix = prefix;
  }
}
