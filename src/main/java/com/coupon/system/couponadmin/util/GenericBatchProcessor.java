package com.coupon.system.couponadmin.util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 어떤 타입(T)의 아이템이든 배치 처리할 수 있는 범용 배치 프로세서
 * 미래에 발생할 다양한 배치 작업을 동일한 '패턴'으로 처리하기 위한 확장성을 염두함 ex. 쿠폰 삭제 등
 * @param <T> 배치 처리할 아이템의 타입
 */
@Getter
public class GenericBatchProcessor<T> {

    private final List<T> itemsToProcess;
    private final int batchSize;
    private int totalCount = 0;

    // [핵심] 어떤 작업을 할 것인지 Consumer(함수형 인터페이스)로 주입받음
    private final Consumer<List<T>> batchAction;

    public GenericBatchProcessor(int batchSize, Consumer<List<T>> batchAction) {
        this.batchSize = batchSize;
        this.batchAction = batchAction;
        this.itemsToProcess = new ArrayList<>(batchSize);
    }

    /**
     * 아이템을 추가하고, 배치 크기에 도달하면 주입받은 작업을 실행함
     */
    public void add(T item) {
        itemsToProcess.add(item);
        totalCount++;
        if (itemsToProcess.size() >= batchSize) {
            flush();
        }
    }

    /**
     * 현재까지 쌓인 아이템에 대해 작업을 실행하고, 내부 리스트를 비움.
     */
    public void flush() {
        if (!itemsToProcess.isEmpty()) {
            batchAction.accept(itemsToProcess); // 주입받은 작업 실행!
            itemsToProcess.clear();
        }
    }

}