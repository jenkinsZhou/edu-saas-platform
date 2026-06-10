package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.order.domain.Coupon;
import com.edusphere.order.mapper.CouponMapper;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponMapper couponMapper;

    public CouponController(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @GetMapping
    @RequirePermission("marketing:coupon:view")
    public ApiResult<PageResult<Coupon>> listCoupons(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {

        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getTenantId, tenantId)
                .eq(Coupon::getDeleted, false);

        if (status != null && !status.isBlank()) {
            wrapper.eq(Coupon::getStatus, status);
        }

        wrapper.orderByDesc(Coupon::getCreatedAt);
        Page<Coupon> result = couponMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping
    @RequirePermission("marketing:coupon:create")
    public ApiResult<Long> createCoupon(@RequestBody @Valid CreateCouponRequest request) {
        Long tenantId = SecurityContext.tenantId();

        Coupon coupon = new Coupon();
        coupon.setTenantId(tenantId);
        coupon.setName(request.name());
        coupon.setCouponType(request.couponType());
        coupon.setDiscountAmount(request.discountAmount());
        coupon.setDiscountPercent(request.discountPercent());
        coupon.setMinOrderAmount(request.minOrderAmount());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidTo(request.validTo());
        coupon.setTotalQuantity(request.totalQuantity());
        coupon.setUsedQuantity(0);
        coupon.setStatus("ACTIVE");
        coupon.setCreatedBy(SecurityContext.accountId());
        couponMapper.insert(coupon);
        return ApiResult.ok(coupon.getId());
    }

    @GetMapping("/available")
    @RequirePermission("marketing:coupon:view")
    public ApiResult<PageResult<Coupon>> getAvailableCoupons(
            @RequestParam BigDecimal orderAmount,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {

        Long tenantId = SecurityContext.tenantId();
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getTenantId, tenantId)
                .eq(Coupon::getStatus, "ACTIVE")
                .eq(Coupon::getDeleted, false)
                .le(Coupon::getValidFrom, now)
                .ge(Coupon::getValidTo, now)
                .le(Coupon::getMinOrderAmount, orderAmount)
                .apply("used_quantity < total_quantity");

        Page<Coupon> result = couponMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return ApiResult.ok(PageResult.of(result));
    }

    public record CreateCouponRequest(
            @NotBlank String name,
            @NotBlank String couponType,
            BigDecimal discountAmount,
            Integer discountPercent,
            BigDecimal minOrderAmount,
            @NotNull LocalDateTime validFrom,
            @NotNull LocalDateTime validTo,
            @NotNull Integer totalQuantity
    ) {}
}
