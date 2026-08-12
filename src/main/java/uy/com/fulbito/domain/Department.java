package uy.com.fulbito.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "departments")
public class Department {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 3, columnDefinition = "char(3)") private String code;
    @Column(nullable = false, unique = true, length = 60) private String name;
    public String getCode() { return code; }
    public String getName() { return name; }
}
