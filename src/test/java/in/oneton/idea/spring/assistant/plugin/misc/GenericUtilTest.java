package in.oneton.idea.spring.assistant.plugin.misc;

import org.junit.jupiter.api.Test;

class GenericUtilTest {

  @Test
  void updateClassNameAsJavadocHtml() {
    StringBuilder s = new StringBuilder();
    GenericUtil.updateClassNameAsJavadocHtml(
        s,
        "java.util.Map<java.lang.String,java.util.List<org.springframework.cloud.client.DefaultServiceInstance>>"
    );
  }
}