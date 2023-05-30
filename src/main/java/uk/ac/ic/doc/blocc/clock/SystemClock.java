package uk.ac.ic.doc.blocc.clock;

import java.time.Instant;

public class SystemClock implements Clock {

  @Override
  public long now() {
    return Instant.now().getEpochSecond();
  }
}
