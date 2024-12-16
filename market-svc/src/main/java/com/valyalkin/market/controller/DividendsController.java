package com.valyalkin.market.controller;

import com.valyalkin.market.config.exception.NotImplementedException;
import com.valyalkin.market.dividends.DividendsService;
import com.valyalkin.market.dto.DividendsDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/dividends")
public class DividendsController {

    private final DividendsService dividendsService;

    public DividendsController(DividendsService dividendsService) {
        this.dividendsService = dividendsService;
    }

    @GetMapping("/{ticker}")
    @ResponseStatus(code = HttpStatus.OK)
    public DividendsDto history(@PathVariable String ticker) {
        return dividendsService.dividends(ticker);
    }
}
