package fr.imag.adele.apam.util.tracker;

import fr.imag.adele.apam.Component;

/**
 * The {@code ComponentTrackerCustomizer} interface allows to specialized a {@code ComponentTracker}
 *
 */
public interface ComponentTrackerCustomizer<T extends Component> {

    /**
     * A {@code Component} is being added to the {@code ComponentTracker}.
     *
     *
     * @param component The {@code Component} been added to the tracker.
     * @return The object being tracked for the specified {@code Component} or {@code null}
     * if the specified {@code Component} should not be tracked.
     */
    void addingComponent(T component);

    /**
     * A {@code Component} tracked by the {@code ComponentTracker} has been removed.
     *
     *
     * @param component The {@code Component} that has been removed.
     */
    void removedComponent(T component);
}
