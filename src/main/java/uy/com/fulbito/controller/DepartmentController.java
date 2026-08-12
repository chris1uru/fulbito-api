package uy.com.fulbito.controller;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.repository.DepartmentRepository;
import java.util.*;
@RestController @RequestMapping("/api/departments")
public class DepartmentController {
    public record DepartmentResponse(String code,String name) {}
    private final DepartmentRepository repository; public DepartmentController(DepartmentRepository repository){this.repository=repository;}
    @GetMapping public List<DepartmentResponse> list(){return repository.findAll().stream().map(d->new DepartmentResponse(d.getCode(),d.getName())).sorted(Comparator.comparing(DepartmentResponse::name)).toList();}
}
