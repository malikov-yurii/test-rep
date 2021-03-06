package com.malikov.ticketsystem.web.controller.flight;

import com.malikov.ticketsystem.dto.FlightDTO;
import com.malikov.ticketsystem.service.FlightService;
import com.malikov.ticketsystem.util.DateTimeUtil;
import com.malikov.ticketsystem.util.dtoconverter.FlightDTOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Yurii Malikov
 */

@RestController
@RequestMapping(value = "/ajax/anonymous/flight")
public class FlightAnonymousAjaxController {

    @Autowired
    private FlightService flightService;

    @GetMapping
    public ModelMap getFilteredPage(
            @RequestParam(value = "fromDepartureDateTimeCondition") @NotNull
                    @DateTimeFormat(pattern = DateTimeUtil.DATE_TIME_PATTERN) LocalDateTime fromDepartureDateTime,
            @RequestParam(value = "toDepartureDateTimeCondition") @NotNull
                    @DateTimeFormat(pattern = DateTimeUtil.DATE_TIME_PATTERN) LocalDateTime toDepartureDateTime,
            @RequestParam(value = "departureAirportCondition") @Size(min = 2, max = 255) String departureAirportName,
            @RequestParam(value = "arrivalAirportCondition") @Size(min = 2, max = 255) String arrivalAirportName,
            @RequestParam(value = "draw") int draw,
            @RequestParam(value = "start") int startingFrom,
            @RequestParam(value = "length") int pageCapacity) {

        List<FlightDTO> flightDTOs = flightService.getFlightTicketPriceMap(departureAirportName, arrivalAirportName,
                        fromDepartureDateTime, toDepartureDateTime, startingFrom, pageCapacity).entrySet()
                .stream()
                .map(entry -> FlightDTOConverter.asDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        ModelMap model = new ModelMap();
        int dataTableHasNextPageIndicator;

        if (flightDTOs.size() > pageCapacity) {
            flightDTOs.remove(flightDTOs.size() - 1);
        }

        dataTableHasNextPageIndicator = startingFrom + flightDTOs.size() + 1;

        model.put("draw", draw);
        model.put("recordsTotal", dataTableHasNextPageIndicator);
        model.put("recordsFiltered", dataTableHasNextPageIndicator);
        model.put("data", flightDTOs);

        return model;
    }
}
