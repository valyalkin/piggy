package com.valyalkin.market.controller;

import com.valyalkin.market.providers.TickerData;
import com.valyalkin.market.ticker.TickerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/ticker")
public class TickerDataController {

    private final TickerService tickerService;

    public TickerDataController(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    @GetMapping("/{ticker}")
    public TickerData tickerData(@PathVariable String ticker) {
        return tickerService.getTickerData(ticker);
    }
}
