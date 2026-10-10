package com.wildlife.patrol.dto;

import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        Long areaId,
        Long leaderStaffId,
        String leaderName,
        List<StaffRef> members,
        boolean active,
        List<String> warnings) {
}
