package com.example.library.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应体 — 所有分页查询接口统一使用此类包装。
 *
 * <p>包含数据列表和分页元信息，前端可根据 pagination 渲染分页组件。
 *
 * @param <T> 列表元素类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据列表 */
    private List<T> list;

    /** 分页元信息 */
    private Pagination pagination;

    /**
     * 分页元信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        /** 当前页码（从1开始） */
        private int page;
        /** 每页条数 */
        private int size;
        /** 总记录数 */
        private long total;
        /** 总页数 */
        private int totalPages;
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建分页结果。
     *
     * @param list  当前页数据
     * @param page  当前页码
     * @param size  每页条数
     * @param total 总记录数
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> list, int page, int size, long total) {
        int totalPages = (int) Math.ceil((double) total / size);
        return PageResult.<T>builder()
                .list(list != null ? list : Collections.emptyList())
                .pagination(Pagination.builder()
                        .page(page)
                        .size(size)
                        .total(total)
                        .totalPages(totalPages)
                        .build())
                .build();
    }

    /** 返回空的分页结果 */
    public static <T> PageResult<T> empty(int page, int size) {
        return of(Collections.emptyList(), page, size, 0);
    }
}
