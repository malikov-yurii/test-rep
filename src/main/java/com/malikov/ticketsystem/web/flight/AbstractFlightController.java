package com.malikov.ticketsystem.web.flight;

import com.malikov.ticketsystem.model.Airport;
import com.malikov.ticketsystem.service.IAirportService;
import com.malikov.ticketsystem.service.IFlightService;
import com.malikov.ticketsystem.to.FlightTo;
import com.malikov.ticketsystem.util.FlightUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Yurii Malikov
 */
public class AbstractFlightController {

    @Autowired
    IFlightService flightService;

    @Autowired
    IAirportService airportService;

    public List<FlightTo> getAll() {
        return flightService.getAll().stream().map(FlightUtil::asTo).collect(Collectors.toList());
    }

    public List<String> getAirportTosByNameMask(String nameMask) {
        return airportService.getByNameMask(nameMask).stream()
                .map(Airport::getName)
                .collect(Collectors.toList());
    }
}
