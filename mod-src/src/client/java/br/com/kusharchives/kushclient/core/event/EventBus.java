package br.com.kusharchives.kushclient.core.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();
    public <T> EventSubscription subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> unsubscribe(eventType, listener);
    }
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<?>> registered = listeners.get(eventType);
        if (registered != null) { registered.remove(listener); if (registered.isEmpty()) listeners.remove(eventType); }
    }
    @SuppressWarnings("unchecked") public <T> void post(T event) {
        List<Consumer<?>> registered = listeners.get(event.getClass());
        if (registered == null) return;
        for (Consumer<?> rawListener : registered) ((Consumer<T>) rawListener).accept(event);
    }
}
