package com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange;

import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PartCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public record RestPartInfo(
    @NotBlank(message = "Part name is required") String name,
    @NotBlank(message = "Car brand is required") String carBrand,
    @NotBlank(message = "Car model is required") String carModel,
    @NotNull(message = "Car year is required") Year carYear,
    @NotNull(message = "Part category is required") PartCategory partCategory,
    @NotNull(message = "Part condition is required") PartCondition condition,
    @NotNull(message = "Price is required") @Positive(message = "Price must be positive")
        BigDecimal price,
    List<MultipartFile> images) {}
