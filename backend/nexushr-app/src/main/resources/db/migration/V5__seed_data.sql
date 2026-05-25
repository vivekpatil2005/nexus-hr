-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  V5__seed_data.sql                                              ║
-- ║  50-employee seed data with realistic Indian employee data      ║
-- ║  Includes departments, employees, users, salary structures      ║
-- ╚══════════════════════════════════════════════════════════════════╝

-- ─── Seed Departments ───
INSERT INTO departments (id, name, description, parent_id) VALUES
(1, 'Board of Directors', 'Executive leadership', NULL),
(2, 'Engineering', 'Software development and engineering', 1),
(3, 'Human Resources', 'People operations and HR', 1),
(4, 'Finance', 'Financial planning and accounting', 1),
(5, 'Sales & Marketing', 'Revenue generation and brand', 1),
(6, 'Product', 'Product management and design', 1),
(7, 'Operations', 'Business operations and support', 1),
(8, 'Frontend Engineering', 'UI/UX development team', 2),
(9, 'Backend Engineering', 'API and services team', 2),
(10, 'DevOps & Cloud', 'Infrastructure and CI/CD', 2),
(11, 'QA & Testing', 'Quality assurance', 2),
(12, 'Data Science', 'Analytics and ML', 2);

SELECT setval('departments_id_seq', 12);

-- ─── Seed 50 Employees ───
INSERT INTO employees (id, emp_code, first_name, last_name, email, phone, department_id, manager_id, designation, status, hire_date, ctc, gender, city, state, date_of_birth, skills) VALUES
-- C-Suite (3)
(1, 'NEX-0001', 'Rajesh', 'Krishnamurthy', 'rajesh.k@nexushr.com', '9876543210', 1, NULL, 'Chief Executive Officer', 'ACTIVE', '2020-01-15', 5000000.00, 'MALE', 'Mumbai', 'Maharashtra', '1975-03-12', '{"leadership": 10, "strategy": 10, "communication": 9}'),
(2, 'NEX-0002', 'Priya', 'Sharma', 'priya.sharma@nexushr.com', '9876543211', 3, 1, 'Chief People Officer', 'ACTIVE', '2020-02-01', 4200000.00, 'FEMALE', 'Mumbai', 'Maharashtra', '1978-07-22', '{"hr_management": 10, "leadership": 9, "talent_acquisition": 9}'),
(3, 'NEX-0003', 'Vikram', 'Mehta', 'vikram.mehta@nexushr.com', '9876543212', 2, 1, 'Chief Technology Officer', 'ACTIVE', '2020-01-20', 4800000.00, 'MALE', 'Bengaluru', 'Karnataka', '1976-11-05', '{"architecture": 10, "java": 9, "cloud": 9}'),

-- HR Department (5)
(4, 'NEX-0004', 'Ananya', 'Reddy', 'ananya.reddy@nexushr.com', '9876543213', 3, 2, 'HR Manager', 'ACTIVE', '2021-03-15', 1800000.00, 'FEMALE', 'Hyderabad', 'Telangana', '1985-09-18', '{"recruitment": 8, "employee_relations": 9, "compliance": 8}'),
(5, 'NEX-0005', 'Karthik', 'Nair', 'karthik.nair@nexushr.com', '9876543214', 3, 4, 'HR Executive', 'ACTIVE', '2022-06-01', 900000.00, 'MALE', 'Chennai', 'Tamil Nadu', '1992-04-10', '{"recruitment": 7, "onboarding": 8, "payroll": 7}'),
(6, 'NEX-0006', 'Deepa', 'Iyer', 'deepa.iyer@nexushr.com', '9876543215', 3, 4, 'HR Executive', 'ACTIVE', '2022-08-15', 850000.00, 'FEMALE', 'Kochi', 'Kerala', '1993-12-25', '{"benefits": 7, "compliance": 8, "training": 7}'),
(7, 'NEX-0007', 'Suresh', 'Patel', 'suresh.patel@nexushr.com', '9876543216', 3, 2, 'Talent Acquisition Lead', 'ACTIVE', '2021-05-10', 1500000.00, 'MALE', 'Ahmedabad', 'Gujarat', '1987-06-30', '{"sourcing": 9, "interviewing": 9, "employer_branding": 8}'),
(8, 'NEX-0008', 'Meera', 'Joshi', 'meera.joshi@nexushr.com', '9876543217', 3, 7, 'Technical Recruiter', 'ACTIVE', '2023-01-10', 750000.00, 'FEMALE', 'Pune', 'Maharashtra', '1995-02-14', '{"sourcing": 7, "screening": 8, "ats": 7}'),

-- Engineering - Backend (8)
(9, 'NEX-0009', 'Arjun', 'Desai', 'arjun.desai@nexushr.com', '9876543218', 9, 3, 'Engineering Manager', 'ACTIVE', '2020-06-01', 3200000.00, 'MALE', 'Bengaluru', 'Karnataka', '1982-01-20', '{"java": 10, "spring_boot": 9, "microservices": 9, "system_design": 9}'),
(10, 'NEX-0010', 'Sneha', 'Gupta', 'sneha.gupta@nexushr.com', '9876543219', 9, 9, 'Senior Software Engineer', 'ACTIVE', '2021-01-15', 2400000.00, 'FEMALE', 'Bengaluru', 'Karnataka', '1988-08-05', '{"java": 9, "spring_boot": 9, "postgresql": 8, "redis": 8}'),
(11, 'NEX-0011', 'Rahul', 'Verma', 'rahul.verma@nexushr.com', '9876543220', 9, 9, 'Senior Software Engineer', 'ACTIVE', '2021-04-01', 2200000.00, 'MALE', 'Noida', 'Uttar Pradesh', '1989-10-15', '{"java": 9, "kubernetes": 8, "aws": 8, "spring_boot": 8}'),
(12, 'NEX-0012', 'Kavitha', 'Sundaram', 'kavitha.s@nexushr.com', '9876543221', 9, 9, 'Software Engineer', 'ACTIVE', '2022-07-01', 1600000.00, 'FEMALE', 'Chennai', 'Tamil Nadu', '1993-03-22', '{"java": 7, "spring_boot": 7, "sql": 8, "rest_api": 7}'),
(13, 'NEX-0013', 'Aditya', 'Kulkarni', 'aditya.kulkarni@nexushr.com', '9876543222', 9, 9, 'Software Engineer', 'ACTIVE', '2023-01-15', 1400000.00, 'MALE', 'Pune', 'Maharashtra', '1995-07-08', '{"java": 7, "spring": 6, "hibernate": 7, "git": 8}'),
(14, 'NEX-0014', 'Divya', 'Menon', 'divya.menon@nexushr.com', '9876543223', 9, 10, 'Junior Software Engineer', 'ACTIVE', '2023-06-01', 1000000.00, 'FEMALE', 'Thiruvananthapuram', 'Kerala', '1997-11-30', '{"java": 6, "sql": 6, "html_css": 7, "git": 6}'),
(15, 'NEX-0015', 'Nikhil', 'Rao', 'nikhil.rao@nexushr.com', '9876543224', 9, 10, 'Junior Software Engineer', 'PROBATION', '2024-01-10', 900000.00, 'MALE', 'Bengaluru', 'Karnataka', '1998-05-18', '{"java": 5, "spring_boot": 5, "sql": 6, "algorithms": 7}'),
(16, 'NEX-0016', 'Pooja', 'Agarwal', 'pooja.agarwal@nexushr.com', '9876543225', 9, 11, 'Software Engineer', 'ACTIVE', '2022-09-01', 1500000.00, 'FEMALE', 'Delhi', 'Delhi', '1994-09-12', '{"java": 8, "kafka": 7, "docker": 7, "testing": 8}'),

-- Engineering - Frontend (6)
(17, 'NEX-0017', 'Sanjay', 'Bhatt', 'sanjay.bhatt@nexushr.com', '9876543226', 8, 3, 'Frontend Lead', 'ACTIVE', '2020-08-01', 2800000.00, 'MALE', 'Bengaluru', 'Karnataka', '1984-04-25', '{"react": 10, "typescript": 9, "css": 9, "system_design": 8}'),
(18, 'NEX-0018', 'Ishita', 'Das', 'ishita.das@nexushr.com', '9876543227', 8, 17, 'Senior Frontend Engineer', 'ACTIVE', '2021-07-15', 2000000.00, 'FEMALE', 'Kolkata', 'West Bengal', '1990-12-03', '{"react": 9, "typescript": 8, "tailwind": 9, "nextjs": 8}'),
(19, 'NEX-0019', 'Amit', 'Sinha', 'amit.sinha@nexushr.com', '9876543228', 8, 17, 'Frontend Engineer', 'ACTIVE', '2022-03-01', 1600000.00, 'MALE', 'Patna', 'Bihar', '1993-08-17', '{"react": 8, "javascript": 8, "css": 7, "figma": 7}'),
(20, 'NEX-0020', 'Ritu', 'Saxena', 'ritu.saxena@nexushr.com', '9876543229', 8, 17, 'Frontend Engineer', 'ACTIVE', '2022-11-01', 1400000.00, 'FEMALE', 'Jaipur', 'Rajasthan', '1994-06-09', '{"react": 7, "vue": 7, "css": 8, "accessibility": 7}'),
(21, 'NEX-0021', 'Vivek', 'Chauhan', 'vivek.chauhan@nexushr.com', '9876543230', 8, 18, 'Junior Frontend Engineer', 'ACTIVE', '2023-08-01', 900000.00, 'MALE', 'Lucknow', 'Uttar Pradesh', '1997-01-28', '{"html": 7, "css": 7, "javascript": 6, "react": 5}'),
(22, 'NEX-0022', 'Nisha', 'Thakur', 'nisha.thakur@nexushr.com', '9876543231', 8, 18, 'Junior Frontend Engineer', 'PROBATION', '2024-02-15', 850000.00, 'FEMALE', 'Chandigarh', 'Punjab', '1998-10-05', '{"react": 5, "typescript": 5, "css": 6, "git": 6}'),

-- DevOps (4)
(23, 'NEX-0023', 'Manish', 'Kumar', 'manish.kumar@nexushr.com', '9876543232', 10, 3, 'DevOps Lead', 'ACTIVE', '2020-09-15', 2600000.00, 'MALE', 'Hyderabad', 'Telangana', '1985-02-20', '{"kubernetes": 9, "aws": 9, "terraform": 9, "ci_cd": 9}'),
(24, 'NEX-0024', 'Lakshmi', 'Venkatesh', 'lakshmi.v@nexushr.com', '9876543233', 10, 23, 'DevOps Engineer', 'ACTIVE', '2022-01-10', 1800000.00, 'FEMALE', 'Bengaluru', 'Karnataka', '1991-07-14', '{"docker": 8, "kubernetes": 8, "jenkins": 7, "aws": 7}'),
(25, 'NEX-0025', 'Gaurav', 'Tiwari', 'gaurav.tiwari@nexushr.com', '9876543234', 10, 23, 'Site Reliability Engineer', 'ACTIVE', '2022-05-01', 2000000.00, 'MALE', 'Gurgaon', 'Haryana', '1990-11-08', '{"monitoring": 8, "linux": 9, "python": 7, "terraform": 7}'),
(26, 'NEX-0026', 'Shreya', 'Mishra', 'shreya.mishra@nexushr.com', '9876543235', 10, 23, 'Cloud Engineer', 'ACTIVE', '2023-04-01', 1500000.00, 'FEMALE', 'Indore', 'Madhya Pradesh', '1994-03-19', '{"aws": 7, "gcp": 6, "networking": 7, "security": 7}'),

-- QA (4)
(27, 'NEX-0027', 'Rohit', 'Pandey', 'rohit.pandey@nexushr.com', '9876543236', 11, 3, 'QA Lead', 'ACTIVE', '2021-02-15', 2200000.00, 'MALE', 'Pune', 'Maharashtra', '1986-05-27', '{"test_automation": 9, "selenium": 9, "api_testing": 8, "performance": 8}'),
(28, 'NEX-0028', 'Swati', 'Deshpande', 'swati.deshpande@nexushr.com', '9876543237', 11, 27, 'Senior QA Engineer', 'ACTIVE', '2021-09-01', 1600000.00, 'FEMALE', 'Mumbai', 'Maharashtra', '1990-08-16', '{"playwright": 8, "cypress": 8, "api_testing": 8, "agile": 7}'),
(29, 'NEX-0029', 'Abhishek', 'Singh', 'abhishek.singh@nexushr.com', '9876543238', 11, 27, 'QA Engineer', 'ACTIVE', '2022-10-01', 1200000.00, 'MALE', 'Noida', 'Uttar Pradesh', '1993-04-03', '{"manual_testing": 8, "jira": 7, "sql": 7, "postman": 8}'),
(30, 'NEX-0030', 'Tanvi', 'Kapoor', 'tanvi.kapoor@nexushr.com', '9876543239', 11, 27, 'QA Engineer', 'ACTIVE', '2023-02-01', 1100000.00, 'FEMALE', 'Delhi', 'Delhi', '1995-12-11', '{"automation": 7, "selenium": 7, "python": 6, "sql": 7}'),

-- Data Science (4)
(31, 'NEX-0031', 'Ramesh', 'Babu', 'ramesh.babu@nexushr.com', '9876543240', 12, 3, 'Data Science Lead', 'ACTIVE', '2021-06-01', 3000000.00, 'MALE', 'Bengaluru', 'Karnataka', '1983-09-14', '{"machine_learning": 10, "python": 9, "statistics": 9, "deep_learning": 8}'),
(32, 'NEX-0032', 'Aparna', 'Hegde', 'aparna.hegde@nexushr.com', '9876543241', 12, 31, 'Data Scientist', 'ACTIVE', '2022-02-15', 2000000.00, 'FEMALE', 'Bengaluru', 'Karnataka', '1991-06-22', '{"python": 9, "sklearn": 8, "nlp": 8, "sql": 7}'),
(33, 'NEX-0033', 'Prasad', 'Murthy', 'prasad.murthy@nexushr.com', '9876543242', 12, 31, 'ML Engineer', 'ACTIVE', '2022-08-01', 2200000.00, 'MALE', 'Mysuru', 'Karnataka', '1990-01-30', '{"pytorch": 8, "mlops": 8, "docker": 7, "feature_engineering": 8}'),
(34, 'NEX-0034', 'Gayatri', 'Rangan', 'gayatri.rangan@nexushr.com', '9876543243', 12, 31, 'Data Analyst', 'ACTIVE', '2023-03-15', 1200000.00, 'FEMALE', 'Chennai', 'Tamil Nadu', '1994-10-07', '{"sql": 8, "python": 7, "tableau": 8, "statistics": 7}'),

-- Finance (5)
(35, 'NEX-0035', 'Ashok', 'Bansal', 'ashok.bansal@nexushr.com', '9876543244', 4, 1, 'Finance Director', 'ACTIVE', '2020-03-01', 3500000.00, 'MALE', 'Mumbai', 'Maharashtra', '1977-05-15', '{"financial_planning": 10, "compliance": 9, "taxation": 9}'),
(36, 'NEX-0036', 'Rekha', 'Pillai', 'rekha.pillai@nexushr.com', '9876543245', 4, 35, 'Senior Accountant', 'ACTIVE', '2021-08-01', 1400000.00, 'FEMALE', 'Mumbai', 'Maharashtra', '1988-02-28', '{"accounting": 9, "tally": 8, "gst": 8, "audit": 7}'),
(37, 'NEX-0037', 'Manoj', 'Jain', 'manoj.jain@nexushr.com', '9876543246', 4, 35, 'Accountant', 'ACTIVE', '2022-04-15', 1000000.00, 'MALE', 'Delhi', 'Delhi', '1991-09-20', '{"accounting": 8, "excel": 9, "payroll": 7, "compliance": 7}'),
(38, 'NEX-0038', 'Shilpa', 'Goyal', 'shilpa.goyal@nexushr.com', '9876543247', 4, 35, 'Financial Analyst', 'ACTIVE', '2022-09-01', 1300000.00, 'FEMALE', 'Gurgaon', 'Haryana', '1992-07-13', '{"financial_modeling": 8, "excel": 9, "power_bi": 7, "forecasting": 7}'),
(39, 'NEX-0039', 'Dinesh', 'Choudhary', 'dinesh.c@nexushr.com', '9876543248', 4, 36, 'Accounts Executive', 'ACTIVE', '2023-07-01', 700000.00, 'MALE', 'Jaipur', 'Rajasthan', '1996-04-05', '{"bookkeeping": 7, "tally": 7, "excel": 7, "gst": 6}'),

-- Sales & Marketing (6)
(40, 'NEX-0040', 'Neha', 'Malhotra', 'neha.malhotra@nexushr.com', '9876543249', 5, 1, 'VP Sales & Marketing', 'ACTIVE', '2020-04-15', 3800000.00, 'FEMALE', 'Mumbai', 'Maharashtra', '1980-08-22', '{"sales_strategy": 10, "marketing": 9, "leadership": 9}'),
(41, 'NEX-0041', 'Rakesh', 'Aggarwal', 'rakesh.a@nexushr.com', '9876543250', 5, 40, 'Sales Manager', 'ACTIVE', '2021-05-01', 2000000.00, 'MALE', 'Delhi', 'Delhi', '1986-03-17', '{"b2b_sales": 9, "crm": 8, "negotiation": 9, "presentation": 8}'),
(42, 'NEX-0042', 'Pallavi', 'Bhat', 'pallavi.bhat@nexushr.com', '9876543251', 5, 40, 'Marketing Manager', 'ACTIVE', '2021-06-15', 1800000.00, 'FEMALE', 'Bengaluru', 'Karnataka', '1988-11-09', '{"digital_marketing": 9, "seo": 8, "content_strategy": 8, "analytics": 8}'),
(43, 'NEX-0043', 'Siddharth', 'Rathi', 'siddharth.rathi@nexushr.com', '9876543252', 5, 41, 'Sales Executive', 'ACTIVE', '2022-12-01', 1000000.00, 'MALE', 'Pune', 'Maharashtra', '1994-01-25', '{"sales": 7, "communication": 8, "crm": 7, "cold_calling": 7}'),
(44, 'NEX-0044', 'Aditi', 'Chakraborty', 'aditi.c@nexushr.com', '9876543253', 5, 42, 'Content Writer', 'ACTIVE', '2023-05-01', 800000.00, 'FEMALE', 'Kolkata', 'West Bengal', '1995-06-18', '{"writing": 9, "seo": 7, "social_media": 8, "copywriting": 8}'),
(45, 'NEX-0045', 'Varun', 'Dutta', 'varun.dutta@nexushr.com', '9876543254', 5, 42, 'Digital Marketing Executive', 'ACTIVE', '2023-09-15', 750000.00, 'MALE', 'Guwahati', 'Assam', '1996-12-01', '{"google_ads": 7, "social_media": 7, "analytics": 6, "seo": 6}'),

-- Product (3)
(46, 'NEX-0046', 'Harini', 'Natarajan', 'harini.n@nexushr.com', '9876543255', 6, 1, 'Head of Product', 'ACTIVE', '2020-05-01', 3600000.00, 'FEMALE', 'Chennai', 'Tamil Nadu', '1981-10-30', '{"product_strategy": 10, "roadmap": 9, "analytics": 8, "ux_research": 8}'),
(47, 'NEX-0047', 'Pranav', 'Kamath', 'pranav.kamath@nexushr.com', '9876543256', 6, 46, 'Product Manager', 'ACTIVE', '2021-10-01', 2200000.00, 'MALE', 'Bengaluru', 'Karnataka', '1989-04-14', '{"product_management": 8, "agile": 9, "data_analysis": 7, "user_research": 8}'),
(48, 'NEX-0048', 'Sakshi', 'Tripathi', 'sakshi.tripathi@nexushr.com', '9876543257', 6, 46, 'UX Designer', 'ACTIVE', '2022-06-01', 1600000.00, 'FEMALE', 'Noida', 'Uttar Pradesh', '1992-08-23', '{"figma": 9, "ux_research": 8, "prototyping": 8, "design_systems": 7}'),

-- Operations (2)
(49, 'NEX-0049', 'Arun', 'Prasad', 'arun.prasad@nexushr.com', '9876543258', 7, 1, 'Operations Manager', 'ACTIVE', '2021-01-15', 2000000.00, 'MALE', 'Chennai', 'Tamil Nadu', '1984-12-08', '{"operations": 9, "vendor_management": 8, "procurement": 8, "facilities": 8}'),
(50, 'NEX-0050', 'Bhavna', 'Sethi', 'bhavna.sethi@nexushr.com', '9876543259', 7, 49, 'Admin Executive', 'ACTIVE', '2022-03-01', 700000.00, 'FEMALE', 'Delhi', 'Delhi', '1995-05-20', '{"administration": 7, "coordination": 8, "documentation": 7, "excel": 7}');

SELECT setval('employees_id_seq', 50);

-- ─── Update Department Heads ───
UPDATE departments SET head_id = 1 WHERE id = 1;   -- CEO -> Board
UPDATE departments SET head_id = 3 WHERE id = 2;   -- CTO -> Engineering
UPDATE departments SET head_id = 2 WHERE id = 3;   -- CPO -> HR
UPDATE departments SET head_id = 35 WHERE id = 4;  -- Finance Director -> Finance
UPDATE departments SET head_id = 40 WHERE id = 5;  -- VP Sales -> Sales
UPDATE departments SET head_id = 46 WHERE id = 6;  -- Head of Product -> Product
UPDATE departments SET head_id = 49 WHERE id = 7;  -- Ops Manager -> Operations
UPDATE departments SET head_id = 17 WHERE id = 8;  -- Frontend Lead -> Frontend
UPDATE departments SET head_id = 9 WHERE id = 9;   -- Eng Manager -> Backend
UPDATE departments SET head_id = 23 WHERE id = 10; -- DevOps Lead -> DevOps
UPDATE departments SET head_id = 27 WHERE id = 11; -- QA Lead -> QA
UPDATE departments SET head_id = 31 WHERE id = 12; -- DS Lead -> Data Science

-- ─── Seed Users (for auth) ───
-- Password: admin123 (BCrypt hash)
INSERT INTO users (id, username, email, password_hash, full_name, enabled, employee_id) VALUES
(1, 'admin', 'rajesh.k@nexushr.com', '$2a$10$PdEZZVoPGwFwqgfGqKAmL.dXXgk.tAp1P1eVA52BVenpiaWGwJKpe', 'Rajesh Krishnamurthy', true, 1),
(2, 'priya.sharma', 'priya.sharma@nexushr.com', '$2a$10$PdEZZVoPGwFwqgfGqKAmL.dXXgk.tAp1P1eVA52BVenpiaWGwJKpe', 'Priya Sharma', true, 2),
(3, 'vikram.mehta', 'vikram.mehta@nexushr.com', '$2a$10$PdEZZVoPGwFwqgfGqKAmL.dXXgk.tAp1P1eVA52BVenpiaWGwJKpe', 'Vikram Mehta', true, 3),
(4, 'ananya.reddy', 'ananya.reddy@nexushr.com', '$2a$10$PdEZZVoPGwFwqgfGqKAmL.dXXgk.tAp1P1eVA52BVenpiaWGwJKpe', 'Ananya Reddy', true, 4),
(5, 'arjun.desai', 'arjun.desai@nexushr.com', '$2a$10$PdEZZVoPGwFwqgfGqKAmL.dXXgk.tAp1P1eVA52BVenpiaWGwJKpe', 'Arjun Desai', true, 9),
(6, 'sneha.gupta', 'sneha.gupta@nexushr.com', '$2a$10$PdEZZVoPGwFwqgfGqKAmL.dXXgk.tAp1P1eVA52BVenpiaWGwJKpe', 'Sneha Gupta', true, 10);

SELECT setval('users_id_seq', 6);

-- Assign roles
INSERT INTO user_roles (user_id, role) VALUES
(1, 'ADMIN'),
(1, 'HR_MANAGER'),
(2, 'HR_MANAGER'),
(3, 'MANAGER'),
(4, 'HR_MANAGER'),
(5, 'MANAGER'),
(6, 'EMPLOYEE');

-- ─── Seed Salary Structures ───
INSERT INTO salary_structures (employee_id, effective_from, basic, hra, da, special_allowance, conveyance, medical, gross, ctc, active) VALUES
(1,  '2024-04-01', 200000, 80000, 20000, 80000, 3200, 1250, 384450, 416667, true),
(2,  '2024-04-01', 168000, 67200, 16800, 67200, 3200, 1250, 323650, 350000, true),
(3,  '2024-04-01', 192000, 76800, 19200, 76800, 3200, 1250, 369250, 400000, true),
(9,  '2024-04-01', 128000, 51200, 12800, 51200, 3200, 1250, 247650, 266667, true),
(10, '2024-04-01', 96000,  38400, 9600,  38400, 3200, 1250, 186850, 200000, true),
(17, '2024-04-01', 112000, 44800, 11200, 44800, 3200, 1250, 217250, 233333, true),
(23, '2024-04-01', 104000, 41600, 10400, 41600, 3200, 1250, 202050, 216667, true),
(31, '2024-04-01', 120000, 48000, 12000, 48000, 3200, 1250, 232450, 250000, true),
(35, '2024-04-01', 140000, 56000, 14000, 56000, 3200, 1250, 270450, 291667, true),
(40, '2024-04-01', 152000, 60800, 15200, 60800, 3200, 1250, 293250, 316667, true);
