package routemap;

import java.util.ArrayList;


//Policy Template.
//For a router, for each neighbor of the router, there are two RouteMap:
//import from this neighbor and export to this neighbor
//The RouteMap are not associated with the nodes on the Pgraph.

public class RouteMapLine {
  public ArrayList<MatchSelectOne> matches;
  public ArrayList<ActionSetOne> actions;
  public String access;

  public RouteMapLine(ArrayList<MatchSelectOne> matches, ArrayList<ActionSetOne> actions, String access) {
    this.matches = matches == null?new ArrayList<>():matches;
    this.actions = actions == null?new ArrayList<>():actions;
    this.access = access;
  }

  public RouteMapLine() {
    this(null, null, null);
  }

  @Override
  public String toString() {
    return "RouteMapLine{" + "matches=" + matches.toString() + ", actions=" + actions.toString() + ", access='" + access
        + '\'' + '}';
  }

  public void generate_template(String host_name, String policy_name,
      Integer lines, String[] match_types, String[] action_types, String networks, String community) {
    for (int i=0; i<lines; i++) {
      MatchSelectOne m = new MatchSelectOne();
      m.generate_template(host_name, match_types, networks, community);
      this.matches.add(m);
      ActionSetOne a = new ActionSetOne();
      a.generate_template(host_name, action_types, community);
      this.actions.add(a);
    }
  }

  public void generate_template(String host_name, String policy_name) {
    this.generate_template(host_name, policy_name, 1, null, null, "1", "1");
  }
}
