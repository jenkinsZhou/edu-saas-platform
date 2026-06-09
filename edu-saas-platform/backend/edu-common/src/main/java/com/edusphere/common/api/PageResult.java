package com.edusphere.common.api;

import java.util.List;
import java.util.Map;

public record PageResult<T>(
        List<T> records,
        long total,
        long page,
        long pageSize,
        Map<String, Object> summary
) {
    public static <T> PageResult<T> of(List<T> records, long total, long page, long pageSize, Map<String, Object> summary) {
        return new PageResult<>(records, total, page, pageSize, summary);
    }
}
