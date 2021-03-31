package uk.tw.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

@Service
public class PricePlanService {

    private final List<PricePlan> pricePlans;
    private final MeterReadingService meterReadingService;
    private final AccountService accountService;

    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService,
        final AccountService accountService) {
    this.pricePlans = pricePlans;
    this.meterReadingService = meterReadingService;
    this.accountService = accountService;
    }

    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        if (!electricityReadings.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(pricePlans.stream().collect(
                Collectors.toMap(PricePlan::getPlanName, t -> calculateCost(electricityReadings.get(), t))));
    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        BigDecimal average = calculateAverageReading(electricityReadings);
        BigDecimal timeElapsed = calculateTimeElapsed(electricityReadings);

        BigDecimal averagedCost = average.divide(timeElapsed, RoundingMode.HALF_UP);
        return averagedCost.multiply(pricePlan.getUnitRate());
    }

    private BigDecimal calculateAverageReading(List<ElectricityReading> electricityReadings) {
        BigDecimal summedReadings = electricityReadings.stream()
                .map(ElectricityReading::getReading)
                .reduce(BigDecimal.ZERO, (reading, accumulator) -> reading.add(accumulator));

        return summedReadings.divide(BigDecimal.valueOf(electricityReadings.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTimeElapsed(List<ElectricityReading> electricityReadings) {
        ElectricityReading first = electricityReadings.stream()
                .min(Comparator.comparing(ElectricityReading::getTime))
                .get();
        ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::getTime))
                .get();

        return BigDecimal.valueOf(Duration.between(first.getTime(), last.getTime()).getSeconds() / 3600.0);
    }

    /**
     * 
     * @see uk.tw.energy.service.PricePlanServiceTest#willReturnCostOverLastWeekForMeter
     * 
     * @param smartMeterId
     * @return
     */
    public Optional<BigDecimal> calculateUsageCostForLastWeek(final String smartMeterId) {
      final String pricePlanId = accountService.getPricePlanIdForSmartMeterId(smartMeterId);
      if (null == pricePlanId)
        return Optional.empty(); // PricePlanNotMappedException
      final Optional<PricePlan> pricePlan = this.getPricePlanFromId(pricePlanId);
      if (!pricePlan.isPresent())
        return Optional.empty(); // PricePlanNotConfiguredException
      final List<ElectricityReading> meterReadings = this.filterMeterReadingsForMeterByDuration(
          smartMeterId, reading -> reading.getTime().isAfter(this.getTimestampForLastWeek()));
      return Optional.of(this.calculateCost(meterReadings, pricePlan.get()));
    }

    private List<ElectricityReading> filterMeterReadingsForMeterByDuration(
        final String smartMeterId, final Predicate<ElectricityReading> criteria) {
      return meterReadingService.getReadings(smartMeterId).orElse(Collections.emptyList()).stream()
          .filter(criteria).collect(Collectors.toList());
    }

    private Optional<PricePlan> getPricePlanFromId(final String pricePlanId) {
      return pricePlans.stream().filter(plan -> pricePlanId.equalsIgnoreCase(plan.getPlanName()))
          .findFirst();
    }

    private Instant getTimestampForLastWeek() {
      return Instant.now().minus(7, ChronoUnit.DAYS);
    }

}
