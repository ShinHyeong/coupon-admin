package com.coupon.system.couponadmin.service.coupon;

import com.coupon.system.couponadmin.domain.coupon.Coupon;
import com.coupon.system.couponadmin.domain.coupon.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CouponBatchService {

    private final CouponRepository couponRepository;

    public CouponBatchService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /**
     * 배치단위로 트랜잭션
     * 성공하면 이 배치만 커밋되고, 실패하면 이 배치만 롤백되는 식으로
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCouponsInNewTransaction(List<Coupon> coupons) {
        couponRepository.saveAll(coupons);
    }
}