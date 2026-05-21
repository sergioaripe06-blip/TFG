package com.sergio.flatshare.features.workspace.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReminderService {
    @NonNull
    public Map<String, Object> buildReminderData(
            @NonNull String concept,
            @NonNull String intervalKey,
            int intervalDays,
            @NonNull Date startAt,
            @NonNull String startDateText,
            @Nullable Date endAt,
            @NonNull String endDateText,
            @NonNull String targetType,
            @NonNull List<String> targetEmails,
            @NonNull String memberEmail,
            @NonNull String primaryRoomId,
            @NonNull String primaryRoomName,
            @NonNull List<String> roomIds,
            @NonNull List<String> roomNames,
            @NonNull String ownerUid,
            @NonNull String ownerEmail,
            @NonNull String groupId,
            @NonNull String groupName
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", concept);
        data.put("interval", intervalKey.toLowerCase(Locale.ROOT));
        data.put("intervalDays", intervalDays);
        data.put("startAt", startAt);
        data.put("startDateText", startDateText);
        data.put("endAt", endAt);
        data.put("endDateText", endAt == null ? "" : endDateText);
        data.put("targetType", targetType.toLowerCase(Locale.ROOT));
        data.put("targetEmails", targetEmails);
        data.put("targetMemberEmail", memberEmail);
        data.put("roomId", primaryRoomId);
        data.put("roomName", primaryRoomName);
        data.put("roomIds", roomIds);
        data.put("roomNames", roomNames);
        data.put("ownerUid", ownerUid);
        data.put("ownerEmail", ownerEmail);
        data.put("groupId", groupId);
        data.put("groupName", groupName);
        return data;
    }
}

