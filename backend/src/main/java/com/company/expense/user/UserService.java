package com.company.expense.user;

import com.company.expense.common.ApiException;
import com.company.expense.common.SecurityUtils;
import com.company.expense.department.Department;
import com.company.expense.department.DepartmentRepository;
import com.company.expense.notification.EmailNotificationService;
import com.company.expense.user.dto.CreateUserRequest;
import com.company.expense.user.dto.UpdateUserRequest;
import com.company.expense.user.dto.UserResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotificationService;

    public UserService(UserRepository userRepository, DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder, EmailNotificationService emailNotificationService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("A user with this email already exists");
        }
        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> ApiException.badRequest("Department not found"));
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()),
                request.fullName(), request.role(), department == null ? null : department.getId());
        user = userRepository.save(user);
        emailNotificationService.notifyAccountCreated(user, request.password());
        return UserResponse.of(user, department == null ? null : department.getName());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        List<User> users = userRepository.findAll();
        Map<Long, String> departmentNames = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
        return users.stream()
                .map(u -> UserResponse.of(u, departmentNames.get(u.getDepartmentId())))
                .sorted(Comparator.comparing(UserResponse::email, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw ApiException.conflict("A user with this email already exists");
        }
        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> ApiException.badRequest("Department not found"));
        }
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setDepartmentId(department == null ? null : department.getId());
        user.setEnabled(request.enabled());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user = userRepository.save(user);
        return UserResponse.of(user, department == null ? null : department.getName());
    }

    @Transactional
    public void deleteUser(Long id) {
        if (id.equals(SecurityUtils.currentUser().getId())) {
            throw ApiException.badRequest("You cannot delete your own account");
        }
        if (!userRepository.existsById(id)) {
            throw ApiException.notFound("User not found");
        }
        userRepository.deleteById(id);
    }
}
