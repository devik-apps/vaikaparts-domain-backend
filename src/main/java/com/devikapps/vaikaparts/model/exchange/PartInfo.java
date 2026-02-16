package com.devikapps.vaikaparts.model.exchange;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.PartCondition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public final class PartInfo {
  @NotNull private String id;
  @NotNull private Part part;
  private String description;
  private double price;
  private PartCondition condition;

  @Override
  public String toString() {
    return format(
        """
        PartInfo={
        \tid=%s,
        \t%s,
        \tdescription=%s,
        \tprice=%s,
        \tcondition=%s
        }\
        """,
        id, part, description, price, condition);
  }
}
