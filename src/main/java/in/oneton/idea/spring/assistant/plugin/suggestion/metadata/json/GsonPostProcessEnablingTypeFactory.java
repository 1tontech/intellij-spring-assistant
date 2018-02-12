package in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

// Courtesy: https://medium.com/@elye.project/post-processing-on-gson-deserialization-26ce5790137d
public class GsonPostProcessEnablingTypeFactory implements TypeAdapterFactory {
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, typeToken);

    return new TypeAdapter<T>() {
      @Override
      public void write(JsonWriter jsonWriter, T value) throws IOException {
        delegateAdapter.write(jsonWriter, value);
      }

      @Override
      public T read(JsonReader jsonReader) throws IOException {
        T obj = delegateAdapter.read(jsonReader);
        if (obj instanceof GsonPostProcessable) {
          GsonPostProcessable.class.cast(obj).doOnGsonDeserialization();
        }
        return obj;
      }
    };
  }
}
