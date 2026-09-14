package com.devikapps.vaikaparts.mapper;

import static com.devikapps.vaikaparts.model.classifier.PartCategory.BATTERY;
import static com.devikapps.vaikaparts.model.classifier.PartCategory.ENGINE_PART;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
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
            .build();

    var persisted = mapper.toPersistence(seller);
    assertEquals(seller.getCategoryList(), persisted.getCategoryList());
    assertEquals(false, persisted.getHandleAllCategory());
    var restored = mapper.toSeller(persisted);
    assertEquals(seller.getCategoryList(), restored.getCategoryList());
    assertEquals(false, restored.getHandleAllCategory());
  }

  @Test
  void defaults_preserve_all_category_notifications() {
    assertTrue(Seller.builder().build().getHandleAllCategory());
    assertTrue(new Seller().getHandleAllCategory());
    assertTrue(JSeller.builder().build().getHandleAllCategory());
    assertTrue(new JSeller().getHandleAllCategory());
    assertTrue(Seller.builder().build().getCategoryList().isEmpty());
    assertTrue(JSeller.builder().build().getCategoryList().isEmpty());
  }
}
