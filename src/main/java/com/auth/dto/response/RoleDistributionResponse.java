package com.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDistributionResponse {
    private List<RoleCount> distribution;

    @Data
    @AllArgsConstructor
    public static class RoleCount {
        private String roleName;
        private long count;
    }
}
