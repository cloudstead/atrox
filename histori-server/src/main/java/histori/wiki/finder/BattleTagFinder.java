package histori.wiki.finder;

import histori.model.NexusTag;
import histori.model.support.RelationshipType;
import histori.model.support.RoleType;
import histori.wiki.WikiNode;
import histori.wiki.WikiNodeType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static histori.wiki.finder.BattleParticipant.commander;
import static histori.wiki.finder.BattleTagFinder.CommanderParseState.seeking_commander;
import static histori.wiki.finder.BattleTagFinder.CommanderParseState.seeking_flag;

@Slf4j
public class BattleTagFinder extends TagFinderBase {

    public static final String INFOBOX_MILITARY_CONFLICT = "Infobox military conflict";
    public static final String HTML_TAG_REGEX = "(<|&lt;)\\s*\\w+\\s*/?\\s*(>|&gt;)";

    public static final int MIN_VALID_NAME_LENGTH = 3;

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
            final String result = trimToFirstLine(infobox.findFirstAttributeWithName("result").findAllChildTextButNotLinkDescriptions());
            tags.add(newTag(result, "result"));
        }

        for (WikiNode child : infobox.getChildren()) {
            if (child.getType() == WikiNodeType.attribute) {
                if (child.getName().startsWith("combatant")) {
                    final String[] combatants = parseCombatants(child);
                    for (String combatant : combatants) {
                        tags.add(newTag(combatant.trim(), "world_actor", "role", RoleType.combatant.name()));
                    }

                    final WikiNode commanderNode = infobox.findFirstAttributeWithName(child.getName().replace("combatant", "commander"));
                    if (commanderNode != null) {
                        final Set<BattleParticipant> commanders = parseCommanders(commanderNode);
                        for (BattleParticipant commander : commanders) {
                            if (commander.isValidName()) {
                                tag = newTag(commander.name, "person", "role", RoleType.commander.name());
                                if (commander.side != null) {
                                    tag.setValue("world_actor", commander.side);
                                } else {
                                    addCombatants(tag, combatants);
                                }
                                tags.add(tag);
                            }
                        }
                    }

                    final WikiNode casualtiesNode = infobox.findFirstAttributeWithName(child.getName().replace("combatant", "casualties"));
                    if (casualtiesNode != null) {
                        if (casualtiesNode.hasSinglePlainlistChild()) {
                            final List<WikiNode> entries = casualtiesNode.findFirstWithType(WikiNodeType.plainlist).getChildren();
                            String activeFlag = null;
                            for (WikiNode plEntry : entries) {
                                if (plEntry.getType().isPlainlistHeader()) {
                                    activeFlag = getFlagName(plEntry);

                                } else if (plEntry.getType().isPlainlistEntry()) {
                                    final String casualty = plEntry.firstChildName();
                                    WikiNode ref = plEntry.findFirstInfoboxWithName("efn");
                                    if (ref == null) ref = plEntry.findFirstInfoboxWithName("sfn");
                                    if (ref != null && hasCasualties(ref)) {
                                        Long[] estimate1 = extractEstimate(casualty);
                                        if (estimate1 != null) {
                                            String continuationAfterRef = plEntry.getChildren().get(2).getName().trim();
                                            if (continuationAfterRef.startsWith("}}")) continuationAfterRef = continuationAfterRef.substring(2).trim();
                                            if (plEntry.getChildren().size() >= 3
                                                    && (continuationAfterRef.startsWith("-")|| continuationAfterRef.startsWith("–"))) {
                                                if (extractEstimate(continuationAfterRef) != null) {
                                                    final NexusTag impactTag = impactTag(activeFlag, estimate1[0].toString() + continuationAfterRef);
                                                    if (impactTag != null) tags.add(impactTag);
                                                }
                                            }
                                        }
                                        for (WikiNode refAttr : ref.getChildren()) {
                                            String refName = refAttr.getName();
                                            if (isValidCasualty(refName)) {
                                                final NexusTag impactTag = impactTag(activeFlag, refName);
                                                if (impactTag != null) tags.add(impactTag);
                                            }
                                        }

                                    } else if (casualty.contains("(") && casualty.contains(")") && casualty.indexOf("(") < casualty.indexOf(")")) {
                                        tags.add(impactTag(activeFlag, casualty, "casualties")); // main casualty tag
                                        String[] parts = casualty.substring(casualty.indexOf('(') + 1, casualty.indexOf(')')).split(",");
                                        for (String part : parts) {
                                            final NexusTag impactTag = impactTag(activeFlag, part.trim());
                                            if (impactTag != null) tags.add(impactTag);
                                        }

                                    } else {
                                        final NexusTag impactTag = impactTag(activeFlag, casualty);
                                        if (impactTag != null) tags.add(impactTag);
                                    }
                                }
                            }
                        } else {
                            final String[] casualties = casualtiesNode.findAllChildTextButNotLinkDescriptions().split(HTML_TAG_REGEX);
                            int validCasualties = 0;
                            for (String casualty : casualties) if (isValidCasualty(casualty)) validCasualties++;
                            final String defaultCasualtyType = validCasualties <= 1 ? "dead" : "casualties";
                            for (String casualty : casualties) {
                                final NexusTag impactTag = impactTag(combatants, casualty, getCasualtyType(casualty, defaultCasualtyType));
                                if (impactTag != null) tags.add(impactTag);
                            }
                        }
                    }
                }
            }
        }

        return tags;
    }

    private boolean hasCasualties(WikiNode ref) {
        if (!ref.hasChildren()) return false;
        for (WikiNode child : ref.getChildren()) {
            final String casualty = child.getName();
            if (isValidCasualty(casualty)) return true;
        }
        return false;
    }

    private boolean isValidCasualty(String casualty) {
        return extractEstimate(casualty) != null && getCasualtyType(casualty, null) != null;
    }

    public NexusTag impactTag(String activeFlag, String casualty) {
        return impactTag(activeFlag, casualty, getCasualtyType(casualty));
    }

    public NexusTag impactTag(String activeFlag, String casualty, String casualtyType) {
        final Long[] estimate = extractEstimate(casualty);
        return newImpactTag(new String[]{activeFlag}, estimate, casualtyType);
    }

    public NexusTag impactTag(String[] activeFlags, String casualty) {
        final Long[] estimate = extractEstimate(casualty);
        if (estimate == null) return null;
        return newImpactTag(activeFlags, estimate, getCasualtyType(casualty));
    }

    public NexusTag impactTag(String[] activeFlags, String casualty, String casualtyType) {
        final Long[] estimate = extractEstimate(casualty);
        if (estimate == null) return null;
        return newImpactTag(activeFlags, estimate, casualtyType);
    }

    private String getCasualtyType(String casualty) { return getCasualtyType(casualty, "dead"); }

    private String getCasualtyType(String casualty, String defaultValue) {

        int ltPos = casualty.indexOf("&lt;");
        if (ltPos != -1) casualty = casualty.substring(0, ltPos);

        final String c = casualty.toLowerCase().trim();
        if (c.contains("killed and wounded") || c.contains("wounded and killed")) return "dead and wounded";
        if (c.contains("dead and wounded") || c.contains("wounded and dead")) return "dead and wounded";
        if (c.contains("killed") || c.contains("dead")) return "dead";
        if (c.contains("wounded") || c.contains("injured")) return "wounded";
        if (c.contains("deserted")) return "deserted";
        if (c.contains("ships sunk or captured") || c.contains("ships captured or sunk")) return "ships sunk or captured";
        if (c.contains("ships sunk")) return "ships sunk";
        if (c.contains("ships captured")) return "ships captured";
        if (c.contains("aircraft lost")) return "aircraft lost";
        if (c.contains("captured or missing") || c.contains("missing or captured")) return "captured or missing";
        if (c.contains("guns captured")) return "guns captured";
        if (c.contains("captured")) return "captured";
        if (c.contains("missing")) return "missing";
        if (c.contains("tanks and assault guns destroyed") || c.contains("assault guns and tanks destroyed")) return "tanks/assault guns destroyed";
        if (c.contains("tanks destroyed")) return "tanks destroyed";
        if (c.contains("assault guns destroyed")) return "assault guns destroyed";
        if (c.contains("casualties") || c.endsWith("total")) return "casualties";
        return defaultValue;
    }

    public String getFlagName(WikiNode node) {
        WikiNode flag;

        flag = node.findFirstWithName(WikiNodeType.infobox, "flag");
        if (flag != null) return flag.firstChildName();

        flag = node.findFirstWithName(WikiNodeType.infobox, "flagicon");
        if (flag != null) return flag.firstChildName();

        flag = node.findFirstWithName(WikiNodeType.infobox, "flagicon image");
        if (flag != null) {
            boolean found = false;
            final List<WikiNode> siblings = article.getParent(node).getChildren();
            for (WikiNode c : siblings) {
                if (found) {
                    if (c.getType().isLink()) return c.firstChildName();
                    return c.findAllChildText();

                } else if (c != flag) continue;
                found = true;
            }
        }
        if (flag != null) return flag.firstChildName();

        return node.findAllChildText();
    }

    public String trimToFirstLine(String result) {
        int pos = result.indexOf("\n");
        if (pos != -1) result = result.substring(0, pos);
        pos = result.toLowerCase().indexOf("<");
        if (pos != -1) result = result.substring(0, pos);
        pos = result.toLowerCase().indexOf("&lt;");
        if (pos != -1) result = result.substring(0, pos);
        return result;
    }

    public String[] parseCombatants(WikiNode targetNode) {
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
                    for (String combatant : child.getName().split(HTML_TAG_REGEX)) {
                        if (trimName(combatant).length() > 0) found.add(combatant.trim());
                    }
                    break;
                case link:
                    String name = child.getName().toLowerCase().trim();
                    if (!name.startsWith("file:") && !name.startsWith("image:")) {
                        for (String combatant : child.getName().split(HTML_TAG_REGEX)) {
                            if (trimName(combatant).length() > 0) found.add(combatant.trim());
                        }
                    }
                    break;

                case plainlist:
                    if (!child.hasChildren()) continue;
                    for (WikiNode entry : child.getChildren()) {
                        if (entry.getType() == WikiNodeType.plainlist_entry) {
                            String flagName = getFlagName(entry);
                            addCombatant(found, flagName);
                        }
                    }
                    break;

                case infobox:
                    if (child.getName().equalsIgnoreCase("plainlist")) {
                        for (WikiNode nestedChild : child.getChildren()) {
                            if (nestedChild.getName().equals("flag")) continue;
                            addCombatant(found, getFlagName(nestedChild));
                            break;
                        }
                    } else if (child.hasChildren()
                            && (child.getName().equalsIgnoreCase("flag")
                             || child.getName().equalsIgnoreCase("flagicon")
                             || child.getName().equalsIgnoreCase("flagicon image"))) {
                        addCombatant(found, getFlagName(child));
                    }

                default: continue;
            }
        }
        return found.toArray(new String[found.size()]);
    }

    public String addCombatant(Set<String> found, String flagText) {
        if (flagText == null) return null;
        flagText = flagText.trim();
        if (flagText.toLowerCase().startsWith("flag of")) flagText = flagText.substring("flag of".length());
        int dotPos = flagText.indexOf('.');
        if (dotPos != -1 && dotPos > flagText.length() - 6) flagText = flagText.substring(0, dotPos);
        if (flagText.length() < 2) return null; // todo: log this?
        flagText = flagText.trim();
        found.add(flagText);
        return flagText;
    }

    enum CommanderParseState {
        seeking_commander, seeking_flag
    }
    public Set<BattleParticipant> parseCommanders(WikiNode targetNode) {
        final Set<BattleParticipant> found = new LinkedHashSet<>();
        boolean skippingComment = false;
        String side = null;
        boolean foundFlags = false;
        CommanderParseState state = seeking_commander;
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
                    if (foundFlags && state != seeking_commander) continue;
                    for (String name : child.getName().split(HTML_TAG_REGEX)) {
                        if (trimName(name).length() > 0) {
                            found.add(commander(name, side));
                            if (foundFlags) state = seeking_flag;
                        }
                    }
                    break;

                case link:
                    if (foundFlags && state != seeking_commander) continue;
                    String name = child.getName().toLowerCase().trim();

                    // If the link is all-chars, this is not a person
                    if (child.hasChildren() && isAllNonWordChars(child.firstChildName())) continue;

                    if (!name.startsWith("file:") && !name.startsWith("image:")) {
                        for (String subname : child.getName().split(HTML_TAG_REGEX)) {
                            if (trimName(subname).length() > 0) {
                                found.add(commander(subname, side));
                                if (foundFlags) state = seeking_flag;
                            }
                        }
                    }
                    break;

                case plainlist:
                    log.info("in plainlist"); break;
                case plainlist_entry:
                    log.info("in plainlist_entry"); break;

                case infobox:
                    if (child.getName().equalsIgnoreCase("plainlist")) {
                        for (WikiNode nestedChild : child.getChildren()) {
                            if (nestedChild.getName().equals("flag")) {
                                foundFlags = true;
                                state = seeking_commander;
                                side = nestedChild.getName().trim();

                            } else {
                                found.add(commander(nestedChild.getName(), side));
                                if (foundFlags) state = seeking_flag;
                            }
                            break;
                        }

                    } else if ((child.getName().equalsIgnoreCase("flag") || child.getName().equalsIgnoreCase("flagicon")) && child.hasChildren()) {
                        foundFlags = true;
                        state = seeking_commander;
                        side = child.firstChildName().trim();
                    }

                default: continue;
            }
        }
        return found;
    }

    private boolean isAllNonWordChars(String name) {
        return trimName(name).length() == 0 && name.trim().length() > 0;
    }

    private String trimName(String name) {
        return name == null ? "" : name.replaceAll("\\W", "").toLowerCase().trim();
    }

    public NexusTag newImpactTag(String[] combatants, Long[] estimate, String tagName) {
        if (estimate == null || estimate.length == 0 || tagName == null) return null; // todo: log this?
        final NexusTag tag;
        switch (estimate.length) {
            case 1:
                tag = newTag(tagName, "impact", "estimate", estimate[0].toString());
                break;
            case 2:
                tag = newTag(tagName, "impact", "low_estimate", estimate[0].toString());
                tag.setValue("high_estimate", estimate[1].toString());
                tag.setValue("estimate", String.valueOf(((estimate[0] + estimate[1]) / 2L)));
                break;
            default:
                log.warn("newTagImpact: invalid estimate: "+estimate);
                return null;
        }
        return addCombatants(tag, combatants);
    }

    public NexusTag addCombatants(NexusTag tag, String[] combatants) {
        for (String combatant : combatants) {
            tag.setValue("world_actor", combatant.trim());
        }
        return tag;
    }

    public static final Pattern FIND_NUMBER_PATTERN = Pattern.compile("\\s*([,\\d]+)(\\s*[-–]\\s*([,\\d]+))?\\s*");

    private Long[] extractEstimate(String val) {
        final Matcher m = FIND_NUMBER_PATTERN.matcher(val);
        if (!m.find()) return null;
        try {
            if (m.groupCount() >= 3) {
                if (m.group(2) == null) {
                    return new Long[]{estimateToLong(m.group(1))};
                } else {
                    return new Long[]{estimateToLong(m.group(1)), estimateToLong(m.group(3))};
                }
            } else {
                log.warn("extractEstimate: error handling '"+val+"'");
                return null;
            }
        } catch (Exception e) {
            log.warn("Error parsing ("+val+"): "+e);
            return null;
        }
    }

    private long estimateToLong(String group) {
        return Long.parseLong(group.trim().replace(",", ""));
    }
}
