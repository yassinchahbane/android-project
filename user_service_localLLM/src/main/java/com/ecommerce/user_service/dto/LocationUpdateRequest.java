package com.ecommerce.user_service.dto;

import lombok.Data;

@Data
public class LocationUpdateRequest {
    private Double latitude;
    private Double longitude;
}
