package com.ibios.web.controller;

import com.ibios.core.profile.ProfileRegistry;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.ProfileSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileQueryController {

  private final ProfileRegistry profileRegistry;

  public ProfileQueryController(ProfileRegistry profileRegistry) {
    this.profileRegistry = profileRegistry;
  }

  @GetMapping
  public ApiResponse<List<ProfileSummary>> list() {
    List<ProfileSummary> summaries =
        profileRegistry.getAll().stream()
            .map(
                p ->
                    new ProfileSummary(
                        p.name(), p.description(), p.provider().name(), p.provider().model()))
            .toList();
    return ApiResponse.ok(summaries);
  }
}
