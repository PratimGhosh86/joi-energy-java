package uk.tw.energy.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.tw.energy.SeedingApplicationDataConfiguration;

public class PricePlanServiceTest {

  private PricePlanService pricePlanService;

  @BeforeEach
  public void setup() {
    pricePlanService = new PricePlanService(new SeedingApplicationDataConfiguration().pricePlans(),
        new MeterReadingService(new HashMap<>()), new AccountService(new HashMap<>()));
  }

  @Test
  public void willReturnEmptyIfNoPricePlanAssociatedWithMeter() {
    assertThat(pricePlanService.calculateUsageCostForLastWeek("")).isEmpty();

  }

  @Test
  public void willReturnCostOverLastWeekForMeter() {
    assertThat(pricePlanService.calculateUsageCostForLastWeek("")).hasToString("");
  }

}
