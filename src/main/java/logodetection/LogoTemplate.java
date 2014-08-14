package logodetection;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_features2d;
import topology.Serializable;

/**
 * Created by Intern04 on 30/7/2014.
 */
public class LogoTemplate implements Comparable<LogoTemplate> {
    opencv_core.Mat imageMat;
    opencv_core.Mat descriptor;
    opencv_features2d.KeyPoint keyPoints;

    Serializable.PatchIdentifier identifier;
    public int priority;

    /* Creates template with given image, key points, descriptor, and identifier */
    public LogoTemplate(opencv_core.Mat mat, opencv_features2d.KeyPoint keyPoints, opencv_core.Mat descriptor, Serializable.PatchIdentifier identifier)
    {
        this.imageMat = mat;
        this.descriptor = descriptor;
        this.keyPoints = keyPoints;
        this.identifier = identifier;
        priority = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogoTemplate that = (LogoTemplate) o;

        if (!identifier.equals(that.identifier)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    public String toString() {
        return "" + priority;
    }

    @Override
    public int compareTo(LogoTemplate o) {
        if (this.priority > o.priority) return -1;
        return 1;
    }
}
