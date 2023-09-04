package org.batfish.allinone;

public class Main {
  public static String osName = System.getProperty("os.name");
  public static void main(String[] args) {
    new AllInOne(args).run();
  }
}
