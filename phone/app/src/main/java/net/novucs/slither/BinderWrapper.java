package net.novucs.slither;

import android.os.Binder;

public class BinderWrapper<T> extends Binder {
    private final T data;

    public BinderWrapper(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}
