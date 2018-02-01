package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.SortedSet;

public class MapClassMetadataProxy extends ClassMetadataProxy {

  MapClassMetadataProxy(@NotNull PsiClassType type) {
    super(type);
  }

  @Nullable
  public SortedSet<Suggestion> findChildKeySuggestionForQueryPrefix(Module module,
      @Nullable String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillMe,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    return doWithTargetAndReturn(target -> {
      assert target instanceof MapClassMetadata;
      return MapClassMetadata.class.cast(target)
          .findChildKeySuggestionForQueryPrefix(module, ancestralKeysDotDelimited,
              matchesRootTillMe, querySegmentPrefixes, querySegmentPrefixStartIndex);
    }, null);
  }

  @Nullable
  public PsiType getMapKeyType() {
    return doWithTargetAndReturn(target -> {
      assert target instanceof MapClassMetadata;
      return MapClassMetadata.class.cast(target).getKeyType();
    }, null);
  }

  @Nullable
  public PsiType getMapValueType() {
    return doWithTargetAndReturn(target -> {
      assert target instanceof MapClassMetadata;
      return MapClassMetadata.class.cast(target).getValueType();
    }, null);
  }

}
