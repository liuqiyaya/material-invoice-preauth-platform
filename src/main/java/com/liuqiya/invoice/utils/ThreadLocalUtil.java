package com.liuqiya.invoice.utils;

public class ThreadLocalUtil {
    //线程本地存储
    private static final ThreadLocal<Object> THREAD_LOCAL = new ThreadLocal<>();
    //根据键设置值
    public static void set(Object value){THREAD_LOCAL.set(value);    }
    //根据键获取值
    public static <T> T get(){return (T) THREAD_LOCAL.get();
    }
    //移除值
    public static void remove(){THREAD_LOCAL.remove();
    }
}
