package com.devikapps.vaikaparts.model.exchange;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.PartCategory;
import java.net.URL;
import java.time.Year;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@SuperBuilder
public final class Part {
  @NotNull private String id;
  @NotNull private String name;
  private String carBrand;
  private String carModel;
  private Year carYear;
  @Nullable private URL imageUrl;
  @NotNull private PartCategory partCategory;

  @Override
  public String toString() {
    return format(
        """
        Part={
        \tid=%s,
        \tname=%s,
        \tcarBrand=%s,
        \tcarModel=%s,
        \tcarYear=%s,
        \timageUrl=%s,
        \tpartCategory=%s
        }\
        """,
        id, name, carBrand, carModel, carYear, imageUrl, partCategory);
  }
}
