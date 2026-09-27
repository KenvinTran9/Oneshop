package com.oneshop.dto.response;

public record AdminDashboardStatsResponse(
        long productCount,
        long storeCount,
        long orderCount,
        long customerCount
) {
}
