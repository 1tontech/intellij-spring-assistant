package in.oneton.idea.spring.assistant.plugin.service;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

// TODO: Fix this
@ExtendWith(MockitoExtension.class)
class SuggestionIndexServiceImplTest {
  @Mock
  Project mockedProject;
  SuggestionIndexServiceImpl suggestionIndexService;

  @BeforeEach
  void setUp() {
    suggestionIndexService = new SuggestionIndexServiceImpl(mockedProject);
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

  class SuggestionIndexServiceImpl
      extends in.oneton.idea.spring.assistant.plugin.service.SuggestionIndexServiceImpl {
    SuggestionIndexServiceImpl(Project project) {
      super();
    }
  }
}
