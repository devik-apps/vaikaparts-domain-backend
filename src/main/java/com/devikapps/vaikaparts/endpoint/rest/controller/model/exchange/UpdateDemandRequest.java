package com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange;

import com.devikapps.vaikaparts.model.classifier.PartCategory;
import java.time.Year;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class UpdateDemandRequest {
  private String description;
  private PartUpdate part;

  @Getter
  @Setter
  @NoArgsConstructor
  public static class PartUpdate {
    private String name;
    private String carBrand;
    private String carModel;
    private Year carYear;
    private PartCategory partCategory;
    private List<MultipartFile> images;
    private List<String> imageKeysToRemove;
  }
}
