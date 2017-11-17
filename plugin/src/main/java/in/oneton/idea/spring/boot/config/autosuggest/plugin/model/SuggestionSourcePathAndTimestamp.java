package in.oneton.idea.spring.boot.config.autosuggest.plugin.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode(of = {"path", "timestamp"})
public class SuggestionSourcePathAndTimestamp {
  private String path;
  private long timestamp;
}
