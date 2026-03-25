package com.devikapps.vaikaparts.config;

import com.devikapps.vaikaparts.pecunia.client.api.AirtelMoneyApi;
import com.devikapps.vaikaparts.pecunia.client.api.MVolaApi;
import com.devikapps.vaikaparts.pecunia.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentClientConf {

  @Value("${payment.base-url}")
  private String baseUrl;

  @Value("${payment.api-key}")
  private String apiKey;

  @Bean
  public ApiClient pecuniaApiClient() {
    final ApiClient client = new ApiClient();
    client.setBasePath(baseUrl);
    client.setApiKey(apiKey);
    return client;
  }

  @Bean
  public MVolaApi mVolaApi(final ApiClient pecuniaApiClient) {
    return new MVolaApi(pecuniaApiClient);
  }

  @Bean
  public AirtelMoneyApi airtelMoneyApi(final ApiClient pecuniaApiClient) {
    return new AirtelMoneyApi(pecuniaApiClient);
  }
}
