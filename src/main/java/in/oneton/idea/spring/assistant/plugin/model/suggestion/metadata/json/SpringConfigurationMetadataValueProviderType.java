package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json;

import com.google.gson.annotations.SerializedName;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#_value_providers
 */
public enum SpringConfigurationMetadataValueProviderType {
  any, @SerializedName("class-reference") class_reference, @SerializedName(
      "handle-as") handle_as, @SerializedName("logger-name") logger_name, @SerializedName(
      "spring-bean-reference") spring_bean_reference, @SerializedName(
      "spring-profile-name") spring_profile_name
}
