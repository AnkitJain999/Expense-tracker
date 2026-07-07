package com.company.expense.department;

import com.company.expense.common.ApiException;
import com.company.expense.department.dto.DepartmentRequest;
import com.company.expense.department.dto.DepartmentResponse;
import com.company.expense.user.Role;
import com.company.expense.user.User;
import com.company.expense.user.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentService(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments() {
        Map<Long, User> usersById = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return departmentRepository.findAll().stream()
                .map(d -> toResponse(d, usersById))
                .sorted(Comparator.comparing(DepartmentResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw ApiException.conflict("A department with this name already exists");
        }
        User teamLead = requireUserWithRole(request.teamLeadId(), Role.TEAM_LEAD, "Team Lead");
        User financeManager = requireUserWithRole(request.financeManagerId(), Role.FINANCE_MANAGER, "Finance Manager");
        Department department = new Department(request.name(),
                teamLead == null ? null : teamLead.getId(),
                financeManager == null ? null : financeManager.getId());
        department = departmentRepository.save(department);
        return toResponse(department, teamLead, financeManager);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Department not found"));
        if (departmentRepository.existsByNameAndIdNot(request.name(), id)) {
            throw ApiException.conflict("A department with this name already exists");
        }
        User teamLead = requireUserWithRole(request.teamLeadId(), Role.TEAM_LEAD, "Team Lead");
        User financeManager = requireUserWithRole(request.financeManagerId(), Role.FINANCE_MANAGER, "Finance Manager");
        department.setName(request.name());
        department.setTeamLeadId(teamLead == null ? null : teamLead.getId());
        department.setFinanceManagerId(financeManager == null ? null : financeManager.getId());
        department = departmentRepository.save(department);
        return toResponse(department, teamLead, financeManager);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw ApiException.notFound("Department not found");
        }
        departmentRepository.deleteById(id);
    }

    private User requireUserWithRole(Long userId, Role role, String label) {
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.badRequest(label + " not found"));
        if (user.getRole() != role) {
            throw ApiException.badRequest(user.getFullName() + " is not a " + label);
        }
        return user;
    }

    private DepartmentResponse toResponse(Department department, Map<Long, User> usersById) {
        return toResponse(department, usersById.get(department.getTeamLeadId()),
                usersById.get(department.getFinanceManagerId()));
    }

    private DepartmentResponse toResponse(Department department, User teamLead, User financeManager) {
        return new DepartmentResponse(department.getId(), department.getName(),
                department.getTeamLeadId(), teamLead == null ? null : teamLead.getFullName(),
                department.getFinanceManagerId(), financeManager == null ? null : financeManager.getFullName());
    }
}
