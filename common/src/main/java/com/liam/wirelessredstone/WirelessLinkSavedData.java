package com.liam.wirelessredstone;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

@SuppressWarnings({"null", "unchecked"})
public final class WirelessLinkSavedData extends SavedData {
    public static final int MAX_RECEIVER_NAME_LENGTH = 32;
    private static final String DEFAULT_RECEIVER_NAME = "receiver";

    private static final Codec<WirelessLinkSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(LinkEntry.CODEC).optionalFieldOf("links", List.of()).forGetter(WirelessLinkSavedData::getLinksRaw),
            Codec.list(ReceiverEntry.CODEC).optionalFieldOf("receivers", List.of()).forGetter(WirelessLinkSavedData::getReceiversRaw),
            Codec.list(TransmitterEntry.CODEC).optionalFieldOf("transmitters", List.of()).forGetter(WirelessLinkSavedData::getTransmittersRaw)
    ).apply(instance, WirelessLinkSavedData::new));

    private static final SavedDataType<WirelessLinkSavedData> TYPE = SavedNetworkType.create(WirelessLinkSavedData::new, CODEC);

    private final Set<LinkEntry> links = new HashSet<>();
    private final Map<String, Long> receiverPositions = new HashMap<>();
    private final Map<Long, String> receiverNamesByPos = new HashMap<>();
    private final Map<String, Long> transmitterPositions = new HashMap<>();
    private final Map<Long, String> transmitterIdsByPos = new HashMap<>();

    private WirelessLinkSavedData() {
    }

    private WirelessLinkSavedData(List<LinkEntry> links, List<ReceiverEntry> receivers, List<TransmitterEntry> transmitters) {
        this.links.addAll(links);

        for (ReceiverEntry receiver : receivers) {
            String receiverName = normalizeReceiverName(receiver.name());

            if (!receiverName.isBlank()) {
                receiverPositions.put(nameKey(receiverName), receiver.pos());
                receiverNamesByPos.put(receiver.pos(), receiverName);
            }
        }

        for (TransmitterEntry transmitter : transmitters) {
            String transmitterId = normalizeTransmitterId(transmitter.id());

            if (!transmitterId.isBlank()) {
                transmitterPositions.put(transmitterId, transmitter.pos());
                transmitterIdsByPos.put(transmitter.pos(), transmitterId);
            }
        }
    }

    public static WirelessLinkSavedData get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public static String normalizeReceiverName(String receiverName) {
        String normalizedName = receiverName == null ? "" : receiverName.trim();

        if (normalizedName.length() > MAX_RECEIVER_NAME_LENGTH) {
            normalizedName = normalizedName.substring(0, MAX_RECEIVER_NAME_LENGTH);
        }

        return normalizedName;
    }

    public static String normalizeTransmitterId(String transmitterId) {
        return transmitterId == null ? "" : transmitterId.trim();
    }

    private static String nameKey(String receiverName) {
        return normalizeReceiverName(receiverName).toLowerCase(Locale.ROOT);
    }

    private List<LinkEntry> getLinksRaw() {
        return List.copyOf(links);
    }

    private List<ReceiverEntry> getReceiversRaw() {
        return receiverNamesByPos.entrySet().stream()
                .map(entry -> new ReceiverEntry(entry.getValue(), entry.getKey().longValue()))
                .toList();
    }

    private List<TransmitterEntry> getTransmittersRaw() {
        return transmitterIdsByPos.entrySet().stream()
                .map(entry -> new TransmitterEntry(entry.getValue(), entry.getKey().longValue()))
                .toList();
    }

    public Set<LinkEntry> getLinks() {
        return Collections.unmodifiableSet(links);
    }

    public Optional<String> getReceiverName(BlockPos receiverPos) {
        return Optional.ofNullable(receiverNamesByPos.get(receiverPos.asLong()));
    }

    public Optional<String> getTransmitterId(BlockPos transmitterPos) {
        return Optional.ofNullable(transmitterIdsByPos.get(transmitterPos.asLong()));
    }

    public Long getReceiverPos(String receiverName) {
        return receiverPositions.get(nameKey(receiverName));
    }

    public Long getTransmitterPos(String transmitterId) {
        return transmitterPositions.get(normalizeTransmitterId(transmitterId));
    }

    public Optional<String> getReceiverNameForTransmitterId(String transmitterId) {
        String safeTransmitterId = normalizeTransmitterId(transmitterId);

        for (LinkEntry link : links) {
            if (link.transmitterId().equals(safeTransmitterId)) {
                return Optional.of(link.receiverName());
            }
        }

        return Optional.empty();
    }

    public String registerReceiver(BlockPos receiverPos, String requestedName) {
        long receiverPacked = receiverPos.asLong();
        String receiverName = normalizeReceiverName(requestedName);

        if (receiverName.isBlank() || !canUseReceiverName(receiverName, receiverPacked)) {
            receiverName = createUniqueReceiverName();
        }

        String oldName = receiverNamesByPos.put(receiverPacked, receiverName);

        if (oldName != null) {
            receiverPositions.remove(nameKey(oldName));
        }

        receiverPositions.put(nameKey(receiverName), receiverPacked);
        setDirty();
        return receiverName;
    }

    public void registerTransmitter(BlockPos transmitterPos, String transmitterId) {
        String safeId = normalizeTransmitterId(transmitterId);

        if (safeId.isBlank()) {
            return;
        }

        long transmitterPacked = transmitterPos.asLong();
        String oldId = transmitterIdsByPos.put(transmitterPacked, safeId);

        if (oldId != null) {
            transmitterPositions.remove(oldId);
        }

        transmitterPositions.put(safeId, transmitterPacked);
        setDirty();
    }

    public RenameResult renameReceiver(BlockPos receiverPos, String requestedName) {
        long receiverPacked = receiverPos.asLong();
        String oldName = receiverNamesByPos.get(receiverPacked);

        if (oldName == null) {
            return RenameResult.failed("", "message.lm_wireless_redstone.receiver_missing");
        }

        String newName = normalizeReceiverName(requestedName);

        if (newName.isBlank()) {
            return RenameResult.failed(oldName, "message.lm_wireless_redstone.receiver_name_empty");
        }

        if (!canUseReceiverName(newName, receiverPacked)) {
            return RenameResult.failed(oldName, "message.lm_wireless_redstone.receiver_name_duplicate");
        }

        if (oldName.equals(newName)) {
            return RenameResult.success(newName, "message.lm_wireless_redstone.receiver_name_saved");
        }

        receiverNamesByPos.put(receiverPacked, newName);
        receiverPositions.remove(nameKey(oldName));
        receiverPositions.put(nameKey(newName), receiverPacked);
        renameReceiverLinks(oldName, newName);
        setDirty();

        return RenameResult.success(newName, "message.lm_wireless_redstone.receiver_name_saved");
    }

    public boolean setLink(String transmitterId, String receiverName) {
        String safeTransmitterId = normalizeTransmitterId(transmitterId);
        String safeReceiverName = receiverNamesByPos.get(getReceiverPosOrFallback(receiverName));

        if (safeTransmitterId.isBlank() || safeReceiverName == null) {
            return false;
        }

        boolean changed = removeLinksFromTransmitterIdInternal(safeTransmitterId) > 0;
        changed |= links.add(new LinkEntry(safeTransmitterId, safeReceiverName));

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public int removeLinksFromTransmitter(BlockPos transmitterPos) {
        String transmitterId = transmitterIdsByPos.get(transmitterPos.asLong());

        if (transmitterId == null) {
            return 0;
        }

        int removedCount = removeLinksFromTransmitterIdInternal(transmitterId);

        if (removedCount > 0) {
            setDirty();
        }

        return removedCount;
    }

    public boolean unregisterTransmitter(BlockPos transmitterPos) {
        long transmitterPacked = transmitterPos.asLong();
        String transmitterId = transmitterIdsByPos.remove(transmitterPacked);
        boolean changed = transmitterId != null;

        if (transmitterId != null) {
            transmitterPositions.remove(transmitterId);
            changed |= removeLinksFromTransmitterIdInternal(transmitterId) > 0;
        }

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public boolean unregisterReceiver(BlockPos receiverPos) {
        long receiverPacked = receiverPos.asLong();
        String receiverName = receiverNamesByPos.remove(receiverPacked);
        boolean changed = receiverName != null;

        if (receiverName != null) {
            receiverPositions.remove(nameKey(receiverName));
            changed |= removeLinksToReceiverNameInternal(receiverName) > 0;
        }

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public boolean removeLinksToReceiver(BlockPos receiverPos) {
        String receiverName = receiverNamesByPos.get(receiverPos.asLong());

        if (receiverName == null) {
            return false;
        }

        int removedCount = removeLinksToReceiverNameInternal(receiverName);

        if (removedCount > 0) {
            setDirty();
        }

        return removedCount > 0;
    }

    public int countTransmittersForReceiver(BlockPos receiverPos) {
        String receiverName = receiverNamesByPos.get(receiverPos.asLong());
        return receiverName == null ? 0 : countTransmittersForReceiverName(receiverName);
    }

    public int countTransmittersForReceiverName(String receiverName) {
        String key = nameKey(receiverName);
        int count = 0;

        for (LinkEntry link : links) {
            if (nameKey(link.receiverName()).equals(key)) {
                count++;
            }
        }

        return count;
    }

    public boolean updateTransmitterPosition(String transmitterId, BlockPos transmitterPos) {
        String safeId = normalizeTransmitterId(transmitterId);

        if (safeId.isBlank()) {
            return false;
        }

        Long oldPos = transmitterPositions.put(safeId, transmitterPos.asLong());

        if (oldPos != null) {
            transmitterIdsByPos.remove(oldPos);
        }

        transmitterIdsByPos.put(transmitterPos.asLong(), safeId);

        if (oldPos == null || oldPos.longValue() != transmitterPos.asLong()) {
            setDirty();
            return true;
        }

        return false;
    }

    public boolean updateReceiverPosition(String receiverName, BlockPos receiverPos) {
        String key = nameKey(receiverName);

        if (key.isBlank()) {
            return false;
        }

        Long oldPos = receiverPositions.put(key, receiverPos.asLong());

        if (oldPos != null) {
            receiverNamesByPos.remove(oldPos);
        }

        receiverNamesByPos.put(receiverPos.asLong(), normalizeReceiverName(receiverName));

        if (oldPos == null || oldPos.longValue() != receiverPos.asLong()) {
            setDirty();
            return true;
        }

        return false;
    }

    public boolean moveBlockPositions(Map<Long, Long> movedTransmitters, Map<Long, Long> movedReceivers) {
        boolean changed = false;

        for (Map.Entry<Long, Long> move : movedTransmitters.entrySet()) {
            String transmitterId = transmitterIdsByPos.remove(move.getKey());

            if (transmitterId != null) {
                transmitterIdsByPos.put(move.getValue(), transmitterId);
                transmitterPositions.put(transmitterId, move.getValue());
                changed = true;
            }
        }

        for (Map.Entry<Long, Long> move : movedReceivers.entrySet()) {
            String receiverName = receiverNamesByPos.remove(move.getKey());

            if (receiverName != null) {
                receiverNamesByPos.put(move.getValue(), receiverName);
                receiverPositions.put(nameKey(receiverName), move.getValue());
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public void updateReceiverCounts(ServerLevel world) {
        for (Map.Entry<Long, String> receiver : receiverNamesByPos.entrySet()) {
            BlockPos receiverPos = BlockPos.of(receiver.getKey().longValue());

            if (world.getBlockEntity(receiverPos) instanceof ReceiverBlockEntity receiverBlockEntity) {
                receiverBlockEntity.setReceiverName(receiver.getValue());
                receiverBlockEntity.setConnectedTransmitters(countTransmittersForReceiverName(receiver.getValue()));
            }
        }

        for (Map.Entry<Long, String> transmitter : transmitterIdsByPos.entrySet()) {
            BlockPos transmitterPos = BlockPos.of(transmitter.getKey().longValue());

            if (world.getBlockEntity(transmitterPos) instanceof TransmitterBlockEntity transmitterBlockEntity) {
                String transmitterId = transmitter.getValue();

                transmitterBlockEntity.setTransmitterId(transmitterId);
                transmitterBlockEntity.setLinkedReceiverName(getReceiverNameForTransmitterId(transmitterId).orElse(""));
            }
        }
    }

    private long getReceiverPosOrFallback(String receiverName) {
        Long receiverPacked = receiverPositions.get(nameKey(receiverName));
        return receiverPacked == null ? Long.MIN_VALUE : receiverPacked.longValue();
    }

    private boolean canUseReceiverName(String receiverName, long receiverPacked) {
        Long existingPos = receiverPositions.get(nameKey(receiverName));
        return existingPos == null || existingPos.longValue() == receiverPacked;
    }

    private String createUniqueReceiverName() {
        int index = 1;

        while (true) {
            String receiverName = DEFAULT_RECEIVER_NAME + "_" + index;

            if (!receiverPositions.containsKey(nameKey(receiverName))) {
                return receiverName;
            }

            index++;
        }
    }

    private void renameReceiverLinks(String oldName, String newName) {
        Set<LinkEntry> updatedLinks = new HashSet<>();
        String oldKey = nameKey(oldName);

        for (LinkEntry link : links) {
            if (nameKey(link.receiverName()).equals(oldKey)) {
                updatedLinks.add(new LinkEntry(link.transmitterId(), newName));
            } else {
                updatedLinks.add(link);
            }
        }

        links.clear();
        links.addAll(updatedLinks);
    }

    private int removeLinksFromTransmitterIdInternal(String transmitterId) {
        int removedCount = 0;
        Iterator<LinkEntry> iterator = links.iterator();

        while (iterator.hasNext()) {
            LinkEntry link = iterator.next();

            if (link.transmitterId().equals(transmitterId)) {
                iterator.remove();
                removedCount++;
            }
        }

        return removedCount;
    }

    private int removeLinksToReceiverNameInternal(String receiverName) {
        String key = nameKey(receiverName);
        int removedCount = 0;
        Iterator<LinkEntry> iterator = links.iterator();

        while (iterator.hasNext()) {
            LinkEntry link = iterator.next();

            if (nameKey(link.receiverName()).equals(key)) {
                iterator.remove();
                removedCount++;
            }
        }

        return removedCount;
    }

    public record RenameResult(boolean success, String receiverName, String messageKey) {
        private static RenameResult success(String receiverName, String messageKey) {
            return new RenameResult(true, receiverName, messageKey);
        }

        private static RenameResult failed(String receiverName, String messageKey) {
            return new RenameResult(false, receiverName, messageKey);
        }
    }

    public record LinkEntry(String transmitterId, String receiverName) {
        private static final Codec<LinkEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("transmitter_id").forGetter(LinkEntry::transmitterId),
                Codec.STRING.fieldOf("receiver_name").forGetter(LinkEntry::receiverName)
        ).apply(instance, LinkEntry::new));
    }

    private record ReceiverEntry(String name, long pos) {
        private static final Codec<ReceiverEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(ReceiverEntry::name),
                Codec.LONG.fieldOf("pos").forGetter(ReceiverEntry::pos)
        ).apply(instance, (String name, Long pos) -> new ReceiverEntry(name, pos.longValue())));
    }

    private record TransmitterEntry(String id, long pos) {
        private static final Codec<TransmitterEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(TransmitterEntry::id),
                Codec.LONG.fieldOf("pos").forGetter(TransmitterEntry::pos)
        ).apply(instance, (String id, Long pos) -> new TransmitterEntry(id, pos.longValue())));
    }
}
