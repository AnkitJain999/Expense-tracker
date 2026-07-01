package com.company.expense.department;

import com.company.expense.department.dto.DepartmentResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the department list for the admin user-creation form. */
@RestController
@RequestMapping("/api/v1/departments")
@PreAuthorize("hasRole('ADMIN')")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public List<DepartmentResponse> list() {
        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentResponse(d.getId(), d.getName()))
                .toList();
    }
}
