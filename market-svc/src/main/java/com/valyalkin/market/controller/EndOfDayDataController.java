package com.valyalkin.market.controller;

import com.valyalkin.market.eod.EndOfDayDataService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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


    }
}
