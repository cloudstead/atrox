package histori.wiki.finder;

import histori.model.NexusTag;
import histori.model.support.RelationshipType;
import histori.model.support.RoleType;
import histori.wiki.WikiNode;
import histori.wiki.WikiNodeType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BattleTagFinder extends TagFinderBase {

    public static final String INFOBOX_MILITARY_CONFLICT = "Infobox military conflict";
    public static final String HTML_LINEBREAK_REGEX = "(<|&lt;)\\s*br\\s*/?\\s*(>|&gt;)";

    @Override public List<NexusTag> find() {

        final List<NexusTag> tags = new ArrayList<>();
        final WikiNode infobox = article.findFirstInfoboxWithName(INFOBOX_MILITARY_CONFLICT);
        if (infobox == null) return tags;

        NexusTag tag;
        tags.add(newTag("battle", "event_type"));

        if (infobox.hasChildNamed("partof")) {
            WikiNode partof = infobox.findChildNamed("partof");
            List<WikiNode> links = partof.getLinks();
            String tagName;
            if (links.isEmpty()) {
                tagName = partof.findAllChildTextButNotLinkDescriptions();
            } else {
                tagName = links.get(0).getName();
            }
            tags.add(newTag(tagName, "event", "relationship", RelationshipType.part_of.name()));
        }

        if (infobox.hasChildNamed("result")) {
            tags.add(newTag(infobox.findFirstAttributeValueWithName("result"), "result"));
        }

        for (WikiNode child : infobox.getChildren()) {
            if (child.getType() == WikiNodeType.attribute) {
                if (child.getName().startsWith("combatant")) {
                    final String[] combatants = child.findAllChildTextButNotLinkDescriptions().split(HTML_LINEBREAK_REGEX);
                    for (String combatant : combatants) {
                        tags.add(newTag(combatant.trim(), "world_actor", "role", RoleType.combatant.name()));
                    }

                    final WikiNode commanderNode = infobox.findFirstAttributeWithName(child.getName().replace("combatant", "commander"));
                    if (commanderNode != null) {
                        final String[] commanders = commanderNode.findAllChildTextButNotLinkDescriptions().split(HTML_LINEBREAK_REGEX);
                        for (String commander : commanders) {
                            tag = newTag(commander.trim(), "person", "role", RoleType.commander.name());
                            addCombatants(tag, combatants);
                            tags.add(tag);
                        }
                    }

                    final WikiNode casualtiesNode = infobox.findFirstAttributeWithName(child.getName().replace("combatant", "casualties"));
                    if (casualtiesNode != null) {
                        final String[] casualties = casualtiesNode.findAllChildTextButNotLinkDescriptions().split(HTML_LINEBREAK_REGEX);
                        for (String casualty : casualties) {

                            final Long estimate = extractFirstNumber(casualty);
                            if (estimate == null) continue;

                            if (casualty.toLowerCase().contains("killed") || casualty.toLowerCase().contains("dead")) {
                                tags.add(newImpactTag(combatants, estimate, "dead"));

                            } else if (casualty.toLowerCase().contains("ships sunk or captured")) {
                                tags.add(newImpactTag(combatants, estimate, "ships sunk or captured"));

                            } else if (casualty.toLowerCase().contains("ships sunk")) {
                                tags.add(newImpactTag(combatants, estimate, "ships sunk"));

                            } else if (casualty.toLowerCase().contains("ships captured")) {
                                tags.add(newImpactTag(combatants, estimate, "ships captured"));

                            } else if (casualty.toLowerCase().contains("casualties")) {
                                tags.add(newImpactTag(combatants, estimate, "casualties"));
                            }
                        }
                    }
                }
            }
        }

        return tags;
    }

    public NexusTag newImpactTag(String[] combatants, Long estimate, String tagName) {
        final NexusTag tag = newTag(tagName, "impact", "estimate", estimate.toString());
        return addCombatants(tag, combatants);
    }

    public NexusTag addCombatants(NexusTag tag, String[] combatants) {
        for (String combatant : combatants) {
            tag.setValue("world_actor", combatant.trim());
        }
        return tag;
    }

    public static final Pattern FIND_NUMBER_PATTERN = Pattern.compile("\\s*([,\\d])+\\s*");

    private Long extractFirstNumber(String val) {
        final Matcher m = FIND_NUMBER_PATTERN.matcher(val);
        if (!m.find()) return null;
        try {
            return Long.parseLong(m.group(0).trim().replace(",", ""));
        } catch (Exception e) {
            log.warn("Error parsing ("+val+"): "+e);
            return null;
        }
    }
}
