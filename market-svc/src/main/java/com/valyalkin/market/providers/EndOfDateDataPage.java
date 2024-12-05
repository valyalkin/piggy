package com.valyalkin.market.providers;

import java.util.List;

public record EndOfDateDataPage(
        List<EndOfDayData> eod,
        Pagination pagination
) {
}
