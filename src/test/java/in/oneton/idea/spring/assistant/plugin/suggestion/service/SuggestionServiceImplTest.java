package in.oneton.idea.spring.assistant.plugin.suggestion.service;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

// TODO: Fix this
@ExtendWith(MockitoExtension.class)
class SuggestionServiceImplTest {
  @Mock
  Project mockedProject;
  SuggestionServiceImpl suggestionIndexService;

  @BeforeEach
  void setUp() {
    suggestionIndexService = new SuggestionServiceImpl(mockedProject);
  }

  @Test
  void canProvideSuggestions() {
  }

  @Test
  void getSuggestions() {
  }

  @Test
  void buildMetadataHierarchy() {
  }

  @Test
  void findDeepestMatch() {
  }

  @Test
  void giveFindSuggestionSource_whenResourceIsPresentInClasspath_thenFindSources() {
    //    mockedModule.
  }

  class SuggestionServiceImpl
      extends in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionServiceImpl {
    SuggestionServiceImpl(Project project) {
      super();
    }
  }
}
