package com.devikapps.vaikaparts.mapper.user;

import com.devikapps.vaikaparts.model.user.Manager;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.ProfilePhotoUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

  private final ProfilePhotoUrlService ppuService;
  private final ValueObjectMapper vom;

  public Researcher toResearcher(JResearcher jResearcher) {
    if (jResearcher == null) return null;

    return Researcher.builder()
        .id(jResearcher.getId())
        .supabaseUserId(jResearcher.getSupabaseUserId())
        .name(jResearcher.getName())
        .phoneNumber(jResearcher.getPhoneNumber())
        .profileImgUrl(ppuService.getPresignedUrl(jResearcher.getProfileImgKey()))
        .userType(jResearcher.getUserType())
        .status(jResearcher.getStatus())
        .createdAt(jResearcher.getCreatedAt())
        .updatedAt(jResearcher.getUpdatedAt())
        .location(vom.map(jResearcher.getLocation()))
        .build();
  }

  public Seller toSeller(JSeller jSeller) {
    if (jSeller == null) return null;

    return Seller.builder()
        .id(jSeller.getId())
        .supabaseUserId(jSeller.getSupabaseUserId())
        .name(jSeller.getName())
        .phoneNumber(jSeller.getPhoneNumber())
        .profileImgUrl(ppuService.getPresignedUrl(jSeller.getProfileImgKey()))
        .userType(jSeller.getUserType())
        .status(jSeller.getStatus())
        .createdAt(jSeller.getCreatedAt())
        .updatedAt(jSeller.getUpdatedAt())
        .garageName(jSeller.getGarageName())
        .location(vom.map(jSeller.getLocation()))
        .latLon(vom.map(jSeller.getLatLon()))
        .build();
  }

  public Manager toManager(JManager jManager) {
    if (jManager == null) return null;

    return Manager.builder()
        .id(jManager.getId())
        .supabaseUserId(jManager.getSupabaseUserId())
        .name(jManager.getName())
        .phoneNumber(jManager.getPhoneNumber())
        .profileImgUrl(ppuService.getPresignedUrl(jManager.getProfileImgKey()))
        .userType(jManager.getUserType())
        .status(jManager.getStatus())
        .createdAt(jManager.getCreatedAt())
        .updatedAt(jManager.getUpdatedAt())
        .role(jManager.getManagerRole())
        .build();
  }
}
