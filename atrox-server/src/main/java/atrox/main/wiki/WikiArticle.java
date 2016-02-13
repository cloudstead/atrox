package atrox.main.wiki;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor @AllArgsConstructor @ToString(of="title") @Slf4j
public class WikiArticle {

    @Getter @Setter private String title;
    @Getter @Setter private String text = "";

    public void addText(String line) { text += line; }

}
