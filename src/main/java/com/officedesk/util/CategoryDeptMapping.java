package com.officedesk.util;

import com.officedesk.enums.DepartmentName;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.Map.entry;

public class CategoryDeptMapping {

    private static final Map<String, DepartmentName> MAPPING = Map.ofEntries(
        entry("Software", DepartmentName.IT),
        entry("Hardware", DepartmentName.IT),
        entry("Network", DepartmentName.IT),
        entry("Access", DepartmentName.IT),
        entry("Payroll", DepartmentName.HR),
        entry("Leave", DepartmentName.HR),
        entry("Policy", DepartmentName.HR),
        entry("Onboarding", DepartmentName.HR),
        entry("Reimbursement", DepartmentName.FINANCE),
        entry("Invoice", DepartmentName.FINANCE),
        entry("Salary", DepartmentName.FINANCE),
        entry("AC", DepartmentName.FACILITIES),
        entry("Cleaning", DepartmentName.FACILITIES),
        entry("Furniture", DepartmentName.FACILITIES),
        entry("Parking", DepartmentName.FACILITIES)
    );

    public static DepartmentName getDept(String category) {
        DepartmentName dept = MAPPING.get(category);
        if (dept == null) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        return dept;
    }

    public static Map<String, DepartmentName> getAllMappings() {
        return MAPPING;
    }
}
