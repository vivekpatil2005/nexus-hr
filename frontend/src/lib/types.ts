export interface Employee {
  id: number;
  empCode: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone: string;
  departmentId: number;
  departmentName: string;
  managerId: number | null;
  managerName: string | null;
  designation: string;
  status: 'ACTIVE' | 'ON_LEAVE' | 'PROBATION' | 'NOTICE_PERIOD' | 'TERMINATED' | 'RESIGNED';
  hireDate: string;
  exitDate: string | null;
  skills: Record<string, number>;
  ctc: number;
  profilePhotoUrl: string | null;
  city: string;
  state: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeSummary {
  id: number;
  empCode: string;
  fullName: string;
  email: string;
  departmentName: string;
  designation: string;
  status: string;
  profilePhotoUrl: string | null;
}

export interface Department {
  id: number;
  name: string;
  description: string;
  parentId: number | null;
  parentName: string | null;
  headId: number | null;
  headName: string | null;
  active: boolean;
  employeeCount: number;
  children: Department[];
}

export interface OrgChartNode {
  id: number;
  empCode: string;
  name: string;
  designation: string;
  departmentName: string;
  profilePhotoUrl: string | null;
  children: OrgChartNode[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface AttendanceRecord {
  id: number;
  employeeId: number;
  employeeName: string;
  empCode: string;
  date: string;
  checkIn: string;
  checkOut: string | null;
  status: 'PRESENT' | 'ABSENT' | 'ON_LEAVE' | 'HALF_DAY' | 'LATE';
  overtimeHours: number;
  notes: string | null;
}

export interface LeaveType {
  id: number;
  name: string;
  description: string | null;
  defaultDays: number;
  carryForward: boolean;
  maxCarryDays: number;
}

export interface LeaveBalance {
  id: number;
  leaveTypeId: number;
  leaveTypeName: string;
  year: number;
  totalDays: number;
  usedDays: number;
  pendingDays: number;
}

export interface LeaveRequest {
  id: number;
  employeeId: number;
  employeeName: string;
  leaveTypeId: number;
  leaveTypeName: string;
  fromDate: string;
  toDate: string;
  totalDays: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  approverId: number | null;
  approverName: string | null;
  reason: string;
  rejectionReason: string | null;
  approvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SalaryStructure {
  id?: number;
  employeeId: number;
  employeeName?: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
  basic: number;
  hra: number;
  da: number;
  specialAllowance: number;
  conveyance: number;
  medical: number;
  lta: number;
  otherAllowances: number;
  gross: number;
  ctc: number;
  active?: boolean;
}

export type PayrollRunStatus = 'DRAFT' | 'PROCESSING' | 'APPROVED' | 'LOCKED' | 'FAILED';

export interface PayrollRun {
  id: number;
  periodMonth: number;
  periodYear: number;
  status: PayrollRunStatus;
  totalGross: number;
  totalDeductions: number;
  totalNet: number;
  employeeCount: number;
  runByUsername: string | null;
  approvedByUsername: string | null;
  notes: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface Payslip {
  id: number;
  payrollRunId: number;
  periodMonth: number;
  periodYear: number;
  employeeId: number;
  empCode: string;
  employeeName: string;
  department: string;
  designation: string;
  basic: number;
  hra: number;
  da: number;
  specialAllowance: number;
  otherEarnings: number;
  gross: number;
  pfEmployee: number;
  pfEmployer: number;
  esiEmployee: number;
  esiEmployer: number;
  professionalTax: number;
  tds: number;
  otherDeductions: number;
  totalDeductions: number;
  netSalary: number;
  deductionsJson: Record<string, any>;
  pdfS3Key: string | null;
  workingDays: number;
  presentDays: number;
  lossOfPayDays: number;
  createdAt: string;
}

export interface ReviewCycle {
  id: number;
  name: string;
  type: 'ANNUAL' | 'SEMI_ANNUAL' | 'QUARTERLY' | 'PROBATION';
  startDate: string;
  endDate: string;
  status: 'DRAFT' | 'ACTIVE' | 'EVALUATION' | 'NORMALIZATION' | 'COMPLETED';
  description: string;
}

export interface Goal {
  id: number;
  employeeId: number;
  employeeName: string;
  reviewCycleId: number;
  reviewCycleName: string;
  parentGoalId: number | null;
  parentGoalTitle: string | null;
  title: string;
  description: string;
  type: 'COMPANY' | 'DEPARTMENT' | 'INDIVIDUAL';
  category: string;
  targetValue: number;
  currentValue: number;
  weight: number;
  unit: string;
  startDate: string;
  dueDate: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'IN_PROGRESS' | 'ACHIEVED' | 'ABANDONED';
  progress: number;
}

export interface PerformanceReview {
  id: number;
  employeeId: number;
  employeeName: string;
  employeeDesignation: string;
  employeeDepartment: string;
  reviewerId: number;
  reviewerName: string;
  cycleId: number;
  cycleName: string;
  reviewType: 'SELF' | 'MANAGER' | 'PEER' | '360';
  goalScore: number;
  competencyScore: number;
  peerScore: number;
  managerScore: number;
  finalScore: number;
  band: string;
  strengths: string;
  improvementAreas: string;
  managerComments: string;
  employeeComments: string;
  status: 'DRAFT' | 'SUBMITTED' | 'ACKNOWLEDGED' | 'ARCHIVED';
  submittedAt: string | null;
  acknowledgedAt: string | null;
}

export interface PeerFeedback {
  id: number;
  reviewCycleId: number;
  reviewCycleName: string;
  fromEmployeeId: number;
  fromEmployeeName: string;
  toEmployeeId: number;
  toEmployeeName: string;
  rating: number;
  strengths: string;
  improvementAreas: string;
  comments: string;
  anonymous: boolean;
  submittedAt: string;
}

export interface NormalizationPreview {
  reviewId: number;
  employeeId: number;
  employeeName: string;
  departmentName: string;
  finalScore: number;
  currentBand: string;
  suggestedBand: string;
}

export interface AttritionPrediction {
  id: number;
  employeeId: number;
  employeeName: string;
  departmentName: string;
  score: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  computedAt: string;
  features: Record<string, any>;
  modelVersion: string;
}

export interface SkillGap {
  employeeId: number;
  employeeName: string;
  designation: string;
  currentSkills: Record<string, number>;
  requiredSkills: Record<string, number>;
  gaps: Record<string, number>;
  recommendations: string[];
}

export interface ChatResponse {
  response: string;
  source: string;
}

