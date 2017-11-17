package in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json;

import lombok.Data;

import javax.annotation.Nullable;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-hints-attributes
 */
@Data
public class SpringConfigurationMetadataValueHint {
  /**
   * A valid value for the element to which the hint refers. If the type of the PROPERTY is an ARRAY, it can also be an ARRAY of value(s). This attribute is mandatory.
   */
  private Object value;
  @Nullable
  private String description;

  @Override
  public String toString() {
    return value.toString();
  }
}
