package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.user.User;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode
@Getter
@Setter
public abstract class Exchange {
  private String id;
  private User author;
  private String description;
  private List<Part> parts;
}
