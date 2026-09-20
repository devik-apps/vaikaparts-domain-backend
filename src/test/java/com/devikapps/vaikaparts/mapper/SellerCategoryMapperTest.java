package com.devikapps.vaikaparts.mapper;

import static com.devikapps.vaikaparts.model.classifier.PartCategory.BATTERY;
import static com.devikapps.vaikaparts.model.classifier.PartCategory.ENGINE_PART;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

class SellerCategoryMapperTest {
  @Test
  void preserves_categories_and_opt_out_through_persistence_mapping() {
    var mapper = Mappers.getMapper(SellerMapper.class);
    ReflectionTestUtils.setField(mapper, "valueObjectMapper", mock(ValueObjectMapper.class));
    ReflectionTestUtils.setField(mapper, "imageUrlMapper", mock(ImageUrlMapper.class));
    var seller =
        Seller.builder()
            .id("seller")
            .categoryList(List.of(ENGINE_PART, BATTERY))
            .handleAllCategory(false)
            .isDeliverying(true)
            .build();

    var persisted = mapper.toPersistence(seller);
    assertEquals(seller.getCategoryList(), persisted.getCategoryList());
    assertEquals(false, persisted.getHandleAllCategory());
    assertTrue(persisted.getIsDeliverying());
    var restored = mapper.toSeller(persisted);
    assertEquals(seller.getCategoryList(), restored.getCategoryList());
    assertEquals(false, restored.getHandleAllCategory());
    assertTrue(restored.getIsDeliverying());
    var json = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .valueToTree(restored);
    assertTrue(json.get("is_deliverying").asBoolean());
  }

  @Test
  void defaults_preserve_all_category_notifications() {
    assertTrue(Seller.builder().build().getHandleAllCategory());
    assertTrue(new Seller().getHandleAllCategory());
    assertTrue(JSeller.builder().build().getHandleAllCategory());
    assertTrue(new JSeller().getHandleAllCategory());
    assertEquals(false, Seller.builder().build().getIsDeliverying());
    assertEquals(false, new Seller().getIsDeliverying());
    assertEquals(false, JSeller.builder().build().getIsDeliverying());
    assertEquals(false, new JSeller().getIsDeliverying());
    assertTrue(Seller.builder().build().getCategoryList().isEmpty());
    assertTrue(JSeller.builder().build().getCategoryList().isEmpty());
  }
}
