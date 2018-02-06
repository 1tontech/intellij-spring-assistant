package in.oneton.idea.spring.assistant.plugin.initializr.misc;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.VersionParser;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.VersionRange;

import java.lang.reflect.Type;

import static java.util.Collections.emptyList;

public class VersionRangeDeserializer implements JsonDeserializer<VersionRange> {

  @Override
  public VersionRange deserialize(JsonElement jsonElement, Type type,
      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    String versionRangeAsStr = jsonElement.getAsString();
    if (versionRangeAsStr != null) {
      return new VersionParser(emptyList()).parseRange(versionRangeAsStr);
    }
    return null;
  }

}
