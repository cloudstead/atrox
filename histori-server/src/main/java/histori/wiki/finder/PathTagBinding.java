package histori.wiki.finder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class PathTagBinding {

    @Getter @Setter private String path;
    @Getter @Setter private String bind;
    @Getter @Setter private PathTagBindingType type = PathTagBindingType.one;

    public PathTagBinding (String path, String bind) { this(path, bind, PathTagBindingType.one); }

    public static PathTagBinding bindPath(String path, String bind) { return bindPath(path, bind, PathTagBindingType.one); }

    public static PathTagBinding bindPath(String path, String bind, PathTagBindingType type) {
        return new PathTagBinding(path, bind, type);
    }
}
