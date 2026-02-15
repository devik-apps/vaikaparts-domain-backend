package com.devikapps.vaikaparts.endpoint.rest.controller;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.ProfilePhotoResponse;
import com.devikapps.vaikaparts.model.user.User;
import com.devikapps.vaikaparts.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public User getCurrentUser() {
    return userService.getCurrentUserResponse();
  }

  @PostMapping("/me/profile-photo")
  public ProfilePhotoResponse uploadProfilePhoto(@RequestParam("photo") MultipartFile photo) {
    String photoUrl = userService.uploadProfilePhoto(photo);
    return new ProfilePhotoResponse(photoUrl);
  }

  @DeleteMapping("/me/profile-photo")
  public ResponseEntity<Void> deleteProfilePhoto() {
    userService.deleteProfilePhoto();
    return ResponseEntity.noContent().build();
  }
}
