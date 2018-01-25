package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-hints-attributes
 */
@Data
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataHint {
  public static final String VALUE_REGEX_FOR_MAP = "\\.values$";

  private String name;
  @Nullable
  private SpringConfigurationMetadataValueHint[] values;
  @Nullable
  private SpringConfigurationMetadataValueProvider[] providers;

  /**
   * If the property that corresponds with this hint represents a map, Hint's key would be end with `.keys`/`.values`
   *
   * @return property name that corresponds to this hint
   */
  public String getExpectedPropertyName() {
    return name.replaceAll("\\.keys$", "").replaceAll(VALUE_REGEX_FOR_MAP, "");
  }

  public boolean representsValueOfMap() {
    return name.endsWith(VALUE_REGEX_FOR_MAP);
  }
}
