package in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class SpringConfigurationMetadataValueProviderTypeDeserializer
    implements JsonDeserializer<SpringConfigurationMetadataValueProviderType> {
  @Override
  public SpringConfigurationMetadataValueProviderType deserialize(JsonElement jsonElement,
      Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    String providerTypeAsStr = jsonElement.getAsString();
    switch (providerTypeAsStr) {
      case "class-reference":
        return SpringConfigurationMetadataValueProviderType.class_reference;
      case "handle-as":
        return SpringConfigurationMetadataValueProviderType.handle_as;
      case "logger-name":
        return SpringConfigurationMetadataValueProviderType.logger_name;
      case "spring-bean-reference":
        return SpringConfigurationMetadataValueProviderType.spring_bean_reference;
      case "spring-profile-name":
        return SpringConfigurationMetadataValueProviderType.spring_profile_name;
      case "any":
        return SpringConfigurationMetadataValueProviderType.any;
      default:
        return SpringConfigurationMetadataValueProviderType.unknown;
    }
  }
}
