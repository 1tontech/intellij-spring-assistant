package in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-hints-attributes
 */
@Data
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataHint {
  private String name;
  @Nullable
  private SpringConfigurationMetadataValueHint[] values;
  @Nullable
  private SpringConfigurationMetadataValueProvider[] providers;
}
