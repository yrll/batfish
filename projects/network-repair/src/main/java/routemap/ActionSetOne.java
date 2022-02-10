package routemap;

public class ActionSetOne {
  public String[] action_types =
      new String[] {"ActionSetLocalPref", "ActionSetAccess", "ActionSetCommunity"};

  public ActionSetLocalPref slp;
  public ActionSetAccess sa;
  public ActionSetCommunity sc;

  public ActionSetOne() {}

  public void generate_template(String hostname, String[] action_types, String community) {
    if (action_types == null || action_types.length == 0) action_types = this.action_types;
    for (int i = 0; i < action_types.length; i++) {
      if (action_types[i].equals("ActionSetLocalPref")) this.slp = new ActionSetLocalPref(null);
      else if (action_types[i].equals("ActionSetCommunity")) this.sc = new ActionSetCommunity();
      else this.sa = new ActionSetAccess("permit");
    }
  }

  static class ActionSetLocalPref {
    public Integer local_pref;

    public ActionSetLocalPref(Integer local_pref) {
      this.local_pref = local_pref;
    }
  }

  static class ActionSetAccess {
    public String access;

    public ActionSetAccess(String access) {
      this.access = access;
    }
  }

  static class ActionSetCommunity {
    public Community community;

    public ActionSetCommunity() {}
  }
}
