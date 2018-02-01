package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType.any;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType.class_reference;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType.handle_as;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType.logger_name;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType.spring_bean_reference;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType.spring_profile_name;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType.unknown;

public class SpringConfigurationMetadataValueProviderTypeDeserializer
    implements JsonDeserializer<SpringConfigurationMetadataValueProviderType> {
  @Override
  public SpringConfigurationMetadataValueProviderType deserialize(JsonElement jsonElement,
      Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    String providerTypeAsStr = jsonElement.getAsString();
    switch (providerTypeAsStr) {
      case "class-reference":
        return class_reference;
      case "handle-as":
        return handle_as;
      case "logger-name":
        return logger_name;
      case "spring-bean-reference":
        return spring_bean_reference;
      case "spring-profile-name":
        return spring_profile_name;
      case "any":
        return any;
      default:
        return unknown;
    }
  }
}
