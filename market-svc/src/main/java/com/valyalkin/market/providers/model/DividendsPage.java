package com.valyalkin.market.providers.model;

import java.util.List;

public record DividendsPage(
        List<Dividend> dividends,
        Pagination pagination
) {
}
