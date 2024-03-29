package org.batfish.representation.juniper;

import org.batfish.datamodel.Prefix6;

public final class Route6FilterLineExact extends Route6FilterLine {

  public Route6FilterLineExact(Prefix6 prefix6) {
    super(prefix6);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof Route6FilterLineExact)) {
      return false;
    }

    Route6FilterLineExact rhs = (Route6FilterLineExact) o;
    return _prefix6.equals(rhs._prefix6);
  }

  @Override
  public int hashCode() {
    return _prefix6.hashCode();
  }
}
