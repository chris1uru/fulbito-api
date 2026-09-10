package uy.com.fulbito.controller;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.VenueDtos.PublicVenueResponse;
import uy.com.fulbito.service.VenueService;
import java.util.*;
@RestController @RequestMapping("/api/public/venues")
public class PublicVenueController {

    private final VenueService service; public PublicVenueController
    (VenueService service) { this.service = service; }

    @GetMapping public List<PublicVenueResponse> list() { return service.publicList(); }

    @GetMapping("/{id}") public PublicVenueResponse one(@PathVariable UUID id) { return service.publicOne(id); }
}
