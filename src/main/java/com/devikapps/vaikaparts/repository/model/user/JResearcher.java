package com.devikapps.vaikaparts.repository.model.user;

import com.devikapps.vaikaparts.repository.model.JLocation;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "researchers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class JResearcher extends JUser {

  @Embedded private JLocation location;
}
