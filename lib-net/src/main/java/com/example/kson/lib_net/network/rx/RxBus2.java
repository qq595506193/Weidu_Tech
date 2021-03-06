package com.example.kson.lib_net.network.rx;

import android.annotation.SuppressLint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.Subject;

/**
 */
public class RxBus2 {
    private static RxBus2 instance;

    /**
     * ConcurrentHashMap: 线程安全集合
     * PublishProcessor 同时充当了Observer和Observable的角色
     */
    @SuppressWarnings("rawtypes")
    private ConcurrentHashMap<Object, List<FlowableProcessor>> subjectMapper = new ConcurrentHashMap<>();

    public static synchronized RxBus2 getInstance() {
        if (null == instance) {
            instance = new RxBus2();
        }
        return instance;
    }

    private RxBus2() {
    }

    /**
     * 订阅事件源
     *
     * @param observable
     * @param consumer
     * @return
     */
    @SuppressLint("CheckResult")
    public RxBus2 onEvent(Observable<?> observable, Consumer<Object> consumer) {
        observable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
        return getInstance();
    }

    /**
     * 注册事件源
     *
     * @param tag key
     * @param <T>
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    public <T> Flowable<T> register(@NonNull Object tag) {
        List<FlowableProcessor> subjectList = subjectMapper.get(tag);
        if (null == subjectList) {
            subjectList = new ArrayList<>();
            subjectMapper.put(tag, subjectList);
        }

        //考虑到多线程原因使用toSerialized方法
        FlowableProcessor<T> processor = (FlowableProcessor<T>) PublishProcessor.create().toSerialized();
        subjectList.add(processor);
        return processor;
    }

    /**
     * 取消整个tag的监听
     *
     * @param tag key
     */
    @SuppressWarnings("rawtypes")
    public void unregister(@NonNull Object tag) {
        List<FlowableProcessor> subjectList = subjectMapper.get(tag);
        if (null != subjectList) {
            subjectMapper.remove(tag);
        }
    }

    /**
     * 取消tag里某个observable的监听
     *
     * @param tag        key
     * @param observable 要删除的observable
     * @return
     */
    @SuppressWarnings("rawtypes")
    public RxBus2 unregister(@NonNull Object tag,
                             @NonNull Observable<?> observable) {
        if (null == observable) {
            return getInstance();
        }

        List<FlowableProcessor> subjectList = subjectMapper.get(tag);
        if (null != subjectList) {
            // 从subjectList中删去observable
            subjectList.remove((Subject<?>) observable);
            // 若此时subjectList为空则从subjectMapper中删去
            if (isEmpty(subjectList)) {
                subjectMapper.remove(tag);
            }
        }
        return getInstance();
    }

    /**
     * 触发事件
     *
     * @param content
     */
    public void post(@NonNull Object content) {
        post(content.getClass().getName(), content);
    }

    /**
     * 触发事件
     *
     * @param tag     key
     * @param content
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void post(@NonNull Object tag, @NonNull Object content) {
        List<FlowableProcessor> subjectList = subjectMapper.get(tag);
        if (!isEmpty(subjectList)) {
            for (FlowableProcessor subject : subjectList) {
                subject.onNext(content);
            }
        }
    }

    /**
     * 判断集合是否为空
     *
     * @param collection 集合
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Collection<FlowableProcessor> collection) {
        return null == collection || collection.isEmpty();
    }
}
