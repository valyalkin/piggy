package com.valyalkin.market.providers.model;

import java.util.List;

public record EndOfDatePage(
        List<EndOfDayPrice> eod,
        Pagination pagination
) {
}
