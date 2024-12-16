package com.valyalkin.market.controller;

import com.valyalkin.market.dto.LatestPriceDto;
import com.valyalkin.market.eod.EndOfDayDataService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/eod")
public class EndOfDayDataController {

    private final EndOfDayDataService endOfDayDataService;

    public EndOfDayDataController(EndOfDayDataService endOfDayDataService) {
        this.endOfDayDataService = endOfDayDataService;
    }

    @PostMapping("/process")
    @ResponseStatus(code = HttpStatus.CREATED)
    public void process(@RequestParam String tickers) {

        final List<String> tickersList = Arrays.asList(tickers.toUpperCase().split(","));
        tickersList.forEach(endOfDayDataService::processDividends);
    }

    @GetMapping("/latest-price/{ticker}")
    @ResponseStatus(code = HttpStatus.OK)
    public LatestPriceDto latestPrice(@PathVariable String ticker) {
        return endOfDayDataService.latestPriceForTicker(ticker);
    }
}
