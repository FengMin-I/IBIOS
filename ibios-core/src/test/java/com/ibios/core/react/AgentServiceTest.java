package com.ibios.core.react;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

  @Mock private ProfileRegistry profileRegistry;
  @Mock private ReActLoop reActLoop;
  @Mock private SessionManager sessionManager;

  private AgentService agentService;

  @BeforeEach
  void setUp() {
    agentService = new AgentService(profileRegistry, reActLoop, sessionManager);
  }

  @Test
  void shouldSetProfileContextDuringProcessing() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    Profile profile = createProfile();
    when(profileRegistry.get("test-profile")).thenReturn(profile);
    when(reActLoop.run(any(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Profile current = ProfileContext.current();
              assertNotNull(current);
              assertEquals("test-profile", current.name());
              return "response";
            });

    agentService.process(session, "Hello");

    verify(reActLoop).run(any(), anyString(), any());
  }

  @Test
  void shouldClearProfileContextOnException() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    Profile profile = createProfile();
    when(profileRegistry.get("test-profile")).thenReturn(profile);
    when(reActLoop.run(any(), anyString(), any())).thenThrow(new RuntimeException("Error"));

    assertThrows(RuntimeException.class, () -> agentService.process(session, "Hello"));

    assertNull(ProfileContext.current());
  }

  @Test
  void shouldPersistSessionAfterProcessing() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    Profile profile = createProfile();
    when(profileRegistry.get("test-profile")).thenReturn(profile);
    when(reActLoop.run(any(), anyString(), any())).thenReturn("response");

    agentService.process(session, "Hello");

    verify(sessionManager).save(session);
  }

  private Profile createProfile() {
    return new Profile(
        "test-profile",
        "test profile",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig("dashscope", "qwen-plus", null),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        10,
        20);
  }
}
