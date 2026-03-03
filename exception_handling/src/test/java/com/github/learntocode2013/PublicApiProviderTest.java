package com.github.learntocode2013;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicApiProviderTest {
  /**
   * At this point, we can see that declaring checked exceptions in the signature of the method in
   * our public APIs has a couple significant advantages:
   *
   * 1) Such an API declares its contract explicitly. The callers can, therefore, reason about the
   * call outcome without looking into the method implementation.
   *
   * 2) The caller won’t be surprised by any unchecked exceptions. It is easier to write error
   * handling code when we know what possible exceptions the called API can throw.
   */

  @Test
  @DisplayName("Test handling checked exceptions in a public API")
  void wrappedIntoUnchecked() {
    try {
      PublicApiProvider.checkProcessorType();
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Test
  @DisplayName("")
  void test() {}
}

