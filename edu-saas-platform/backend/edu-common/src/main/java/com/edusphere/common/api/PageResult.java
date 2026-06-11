package com.edusphere.common.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), null);
    }
}
