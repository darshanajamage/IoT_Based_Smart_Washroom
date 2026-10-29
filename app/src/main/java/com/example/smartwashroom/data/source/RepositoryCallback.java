package com.example.smartwashroom.data.source;

/**
 * Simple callback used by the data sources and the repository.
 *
 * <p>Only {@link #onSuccess(Object)} has to be implemented, so the callback can
 * be written as a short lambda in the screens:</p>
 *
 * <pre>
 * repository.loadLatestAlert(record -&gt; showRecord(record));
 * </pre>
 *
 * @param <T> the type of the value that is delivered on success
 */
public interface RepositoryCallback<T> {

    /** Called when the operation finished successfully. */
    void onSuccess(T result);

    /**
     * Called when the operation failed.
     *
     * <p>The default implementation does nothing, because most screens can
     * simply keep showing what they already show.</p>
     */
    default void onError(Exception error) {
        // Nothing to do by default.
    }
}
