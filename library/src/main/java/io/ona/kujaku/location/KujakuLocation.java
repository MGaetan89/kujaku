package io.ona.kujaku.location;

import android.location.Location;
import android.support.annotation.NonNull;

/**
 * KujakuLocation to extends the default Android Location by adding some attributes
 *
 * Created by Emmanuel Otin - eo@novel-t.ch 17/07/19.
 */
public class KujakuLocation extends Location {

    private long tag;

    public KujakuLocation(@NonNull Location location, long tag) {
        super(location);
        this.tag = tag;
    }

    /**
     * Get tag
     *
     * @return the tag
     */
    public long getTag() {
        return tag;
    }
}
