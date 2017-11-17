package in.oneton.idea.spring.boot.config.autosuggest.plugin.service;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import java.io.IOException;

// TODO: Fix this
@ExtendWith(MockitoExtension.class)
class SuggestionIndexServiceImplTest {
  @Mock
  Project mockedProject;
  SuggestionIndexServiceImpl suggestionIndexService;

  @BeforeEach
  void setUp() throws IOException {
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

  class SuggestionIndexServiceImpl extends
      in.oneton.idea.spring.boot.config.autosuggest.plugin.service.SuggestionIndexServiceImpl {
    SuggestionIndexServiceImpl(Project project) {
      super();
    }
  }
}
