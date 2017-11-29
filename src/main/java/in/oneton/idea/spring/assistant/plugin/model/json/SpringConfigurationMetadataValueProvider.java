package in.oneton.idea.spring.assistant.plugin.model.json;

import lombok.Data;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-hints-attributes
 */
@Data
public class SpringConfigurationMetadataValueProvider {
  /**
   * The name of the provider to use to offer additional content assistance for the element to which the hint refers.
   */
  private String name;
  /**
   * Any additional parameter that the provider supports (check the documentation of the provider for more details).
   */
  private Object parameters;
}
