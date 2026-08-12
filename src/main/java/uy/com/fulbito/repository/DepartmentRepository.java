package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.Department;
public interface DepartmentRepository extends JpaRepository<Department, String> {}
