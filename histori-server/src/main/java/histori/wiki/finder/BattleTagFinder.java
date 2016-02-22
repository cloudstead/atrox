package histori.wiki.finder;

import histori.model.NexusTag;
import histori.model.support.RelationshipType;
import histori.model.support.RoleType;
import histori.wiki.WikiNode;
import histori.wiki.WikiNodeType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
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
            final String result = trimToFirstLine(infobox.findFirstAttributeValueWithName("result"));
            tags.add(newTag(result, "result"));
        }

        for (WikiNode child : infobox.getChildren()) {
            if (child.getType() == WikiNodeType.attribute) {
                if (child.getName().startsWith("combatant")) {
                    final String[] combatants = parseTagNames(child);
                    for (String combatant : combatants) {
                        tags.add(newTag(combatant.trim(), "world_actor", "role", RoleType.combatant.name()));
                    }

                    final WikiNode commanderNode = infobox.findFirstAttributeWithName(child.getName().replace("combatant", "commander"));
                    if (commanderNode != null) {
                        final String[] commanders = parseTagNames(commanderNode);
                        for (String commander : commanders) {
                            if (trimName(commander).length() > 0) {
                                tag = newTag(commander.trim(), "person", "role", RoleType.commander.name());
                                addCombatants(tag, combatants);
                                tags.add(tag);
                            }
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

                            } else if (casualty.toLowerCase().contains("captured")) {
                                tags.add(newImpactTag(combatants, estimate, "captured"));

                            } else {
                                tags.add(newImpactTag(combatants, estimate, "dead"));
                            }
                        }
                    }
                }
            }
        }

        return tags;
    }

    public String trimToFirstLine(String result) {
        int pos = result.indexOf("\n");
        if (pos != -1) result = result.substring(0, pos);
        pos = result.toLowerCase().indexOf("<br>");
        if (pos != -1) result = result.substring(0, pos);
        pos = result.toLowerCase().indexOf("&lt;br&gt;");
        if (pos != -1) result = result.substring(0, pos);
        pos = result.toLowerCase().indexOf("&lt;br/&gt;");
        if (pos != -1) result = result.substring(0, pos);
        return result;
    }

    public String[] parseTagNames(WikiNode targetNode) {
        final Set<String> found = new LinkedHashSet<>();
        boolean skippingComment = false;
        for (WikiNode child : targetNode.getChildren()) {
            switch (child.getType()) {
                case string:
                    // detect and skip HTML comments
                    if (child.getName().trim().startsWith("&lt;!--")) {
                        skippingComment = true; continue;
                    }
                    if (child.getName().trim().endsWith("--&gt;")) {
                        skippingComment = false; continue;
                    }
                    if (skippingComment) continue;
                    for (String combatant : child.getName().split(HTML_LINEBREAK_REGEX)) {
                        if (trimName(combatant).length() > 0) found.add(combatant.trim());
                    }
                    break;
                case link:
                    String name = child.getName().toLowerCase().trim();
                    if (!name.startsWith("file:") && !name.startsWith("image:")) {
                        for (String combatant : child.getName().split(HTML_LINEBREAK_REGEX)) {
                            if (trimName(combatant).length() > 0) found.add(combatant.trim());
                        }
                    }
                    break;
                case infobox:
                    if (child.getName().equalsIgnoreCase("plainlist")) {
                        for (WikiNode nestedChild : child.getChildren()) {
                            if (nestedChild.getName().equals("flag")) continue;
                            found.add(nestedChild.getName().trim());
                            break;
                        }
                    } else if (child.getName().equalsIgnoreCase("flag") && child.hasChildren()) {
                        found.add(child.getChildren().get(0).getName().trim());
                    }

                default: continue;
            }
        }
        return found.toArray(new String[found.size()]);
    }

    private String trimName(String combatant) {
        return combatant.replaceAll("\\W", "").toLowerCase().trim();
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
