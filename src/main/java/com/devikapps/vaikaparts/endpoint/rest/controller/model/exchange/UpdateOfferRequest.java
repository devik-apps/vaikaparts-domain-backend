package com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange;

import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PartCondition;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class UpdateOfferRequest {
  private String description;
  private PartInfoUpdate partInfo;

  @Getter
  @Setter
  @NoArgsConstructor
  public static class PartInfoUpdate {
    private String name;
    private String carBrand;
    private String carModel;
    private Year carYear;
    private PartCategory partCategory;
    private PartCondition condition;
    private BigDecimal price;
    private List<MultipartFile> images;
  }
}
