/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
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
