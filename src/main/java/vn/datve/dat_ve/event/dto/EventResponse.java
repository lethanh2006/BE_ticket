package vn.datve.dat_ve.event.dto;

import vn.datve.dat_ve.event.Event;

public record EventResponse(
        Long id,
        String name,
        String description,
        String venue
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getVenue()
        );
    }
}