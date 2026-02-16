package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.devikapps.vaikaparts.model.classifier.PartCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Year;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public record RestPart(
    @NotBlank(message = "Part name is required") String name,
    @NotBlank(message = "Car brand is required") String carBrand,
    @NotBlank(message = "Car model is required") String carModel,
    @NotNull(message = "Car year is required") Year carYear,
    List<MultipartFile> images,
    @NotNull(message = "Part category is required") PartCategory partCategory) {}
