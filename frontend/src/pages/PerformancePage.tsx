import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../lib/auth';
import api from '../lib/api';
import type { Goal, PerformanceReview, ReviewCycle, PeerFeedback, NormalizationPreview, ApiResponse } from '../lib/types';
import {
  Target, Award, Users, ShieldAlert, Plus, Check, Edit2,
  TrendingUp, Award as AwardIcon, Star, CheckCircle
} from 'lucide-react';
import toast from 'react-hot-toast';
import './PerformancePage.css';

export default function PerformancePage() {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'goals' | 'reviews' | 'feedback' | 'normalization'>('goals');
  const [selectedCycleId, setSelectedCycleId] = useState<number | null>(null);

  // Goal Form State
  const [showGoalModal, setShowGoalModal] = useState(false);
  const [editingGoal, setEditingGoal] = useState<Goal | null>(null);
  const [goalTitle, setGoalTitle] = useState('');
  const [goalDesc, setGoalDesc] = useState('');
  const [goalType, setGoalType] = useState<'COMPANY' | 'DEPARTMENT' | 'INDIVIDUAL'>('INDIVIDUAL');
  const [goalCategory, setGoalCategory] = useState('');
  const [targetVal, setTargetVal] = useState(100);
  const [currentVal, setCurrentVal] = useState(0);
  const [weight, setWeight] = useState(10);
  const [unit, setUnit] = useState('%');
  const [startDate, setStartDate] = useState('');
  const [dueDate, setDueDate] = useState('');

  // Review Form State
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [selectedReview, setSelectedReview] = useState<PerformanceReview | null>(null);
  const [reviewGoalScore, setReviewGoalScore] = useState(3.0);
  const [reviewCompScore, setReviewCompScore] = useState(3.0);
  const [reviewStrengths, setReviewStrengths] = useState('');
  const [reviewImprovement, setReviewImprovement] = useState('');
  const [reviewComments, setReviewComments] = useState('');

  // Feedback Form State
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [feedbackToEmpId, setFeedbackToEmpId] = useState('');
  const [feedbackRating, setFeedbackRating] = useState(4.0);
  const [feedbackStrengths, setFeedbackStrengths] = useState('');
  const [feedbackImprovement, setFeedbackImprovement] = useState('');
  const [feedbackComments, setFeedbackComments] = useState('');
  const [feedbackAnonymous, setFeedbackAnonymous] = useState(false);

  // Fetch cycles
  const { data: cycles } = useQuery<ReviewCycle[]>({
    queryKey: ['reviewCycles'],
    queryFn: () => api.get<ApiResponse<ReviewCycle[]>>('/performance/cycles').then(r => r.data.data),
  });

  const activeCycle = cycles?.find((c) => c.status === 'ACTIVE' || c.status === 'EVALUATION');
  const currentCycleId = selectedCycleId || activeCycle?.id || (cycles && cycles.length > 0 ? cycles[0].id : null);

  // Fetch logged in user's employee ID
  const empId = user?.employeeId || user?.id;

  // Fetch goals
  const { data: goals, isLoading: isLoadingGoals } = useQuery<Goal[]>({
    queryKey: ['employeeGoals', empId, currentCycleId],
    queryFn: () => {
      const url = currentCycleId 
        ? `/goals/employee/${empId}?cycleId=${currentCycleId}` 
        : `/goals/employee/${empId}`;
      return api.get<ApiResponse<Goal[]>>(url).then(r => r.data.data);
    },
    enabled: !!empId,
  });

  // Fetch logged in user's reviews
  const { data: myReviews, isLoading: isLoadingReviews } = useQuery<PerformanceReview[]>({
    queryKey: ['myReviews'],
    queryFn: () => api.get<ApiResponse<PerformanceReview[]>>('/performance/reviews/me').then(r => r.data.data),
  });

  // Fetch feedback for me
  const { data: feedbackForMe } = useQuery<PeerFeedback[]>({
    queryKey: ['feedbackForMe', currentCycleId, empId],
    queryFn: () => api.get<ApiResponse<PeerFeedback[]>>(`/performance/feedbacks/for/${empId}?cycleId=${currentCycleId}`).then(r => r.data.data),
    enabled: !!currentCycleId && !!empId,
  });

  // Normalization Preview
  const { data: normalizationData, refetch: refetchNormalization, isLoading: isLoadingNormalization } = useQuery<NormalizationPreview[]>({
    queryKey: ['normalization', currentCycleId],
    queryFn: () => api.get<ApiResponse<NormalizationPreview[]>>(`/performance/cycles/${currentCycleId}/normalize`).then(r => r.data.data),
    enabled: !!currentCycleId && (hasRole('ROLE_ADMIN') || hasRole('ROLE_HR_MANAGER')),
  });

  // Goal Mutations
  const saveGoalMutation = useMutation({
    mutationFn: (goalData: any) => {
      if (editingGoal) {
        return api.put<ApiResponse<Goal>>(`/goals/${editingGoal.id}`, goalData);
      }
      return api.post<ApiResponse<Goal>>('/goals', goalData);
    },
    onSuccess: () => {
      toast.success(editingGoal ? 'Goal updated successfully!' : 'Goal created successfully!');
      setShowGoalModal(false);
      setEditingGoal(null);
      resetGoalForm();
      queryClient.invalidateQueries({ queryKey: ['employeeGoals'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to save goal');
    }
  });

  const updateGoalProgressMutation = useMutation({
    mutationFn: ({ goalId, progress }: { goalId: number, progress: number }) => {
      // Fetch target goal to update currentValue proportionally
      const targetGoal = goals?.find(g => g.id === goalId);
      const targetValue = targetGoal?.targetValue || 100;
      const computedValue = (progress / 100) * Number(targetValue);
      return api.put<ApiResponse<Goal>>(`/goals/${goalId}`, {
        ...targetGoal,
        currentValue: computedValue
      });
    },
    onSuccess: () => {
      toast.success('Progress updated successfully!');
      queryClient.invalidateQueries({ queryKey: ['employeeGoals'] });
    },
    onError: () => toast.error('Failed to update progress')
  });

  // Submit Review Mutation
  const submitReviewMutation = useMutation({
    mutationFn: (reviewData: any) => api.post<ApiResponse<PerformanceReview>>('/performance/reviews', reviewData),
    onSuccess: () => {
      toast.success('Performance review submitted successfully!');
      setShowReviewModal(false);
      setSelectedReview(null);
      queryClient.invalidateQueries({ queryKey: ['myReviews'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to submit review');
    }
  });

  // Create Cycle Mutation
  const [showCycleModal, setShowCycleModal] = useState(false);
  const [cycleName, setCycleName] = useState('');
  const [cycleStartDate, setCycleStartDate] = useState('');
  const [cycleEndDate, setCycleEndDate] = useState('');

  const createCycleMutation = useMutation({
    mutationFn: (cycleData: any) => api.post<ApiResponse<ReviewCycle>>('/performance/cycles', cycleData),
    onSuccess: () => {
      toast.success('Performance cycle created successfully!');
      setShowCycleModal(false);
      setCycleName('');
      setCycleStartDate('');
      setCycleEndDate('');
      queryClient.invalidateQueries({ queryKey: ['reviewCycles'] });
    },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Failed to create cycle')
  });

  const handleCreateCycle = (e: React.FormEvent) => {
    e.preventDefault();
    createCycleMutation.mutate({
      name: cycleName,
      type: 'QUARTERLY',
      startDate: cycleStartDate,
      endDate: cycleEndDate,
      status: 'ACTIVE'
    });
  };

  // Submit Feedback Mutation
  const submitFeedbackMutation = useMutation({
    mutationFn: (feedbackData: any) => api.post<ApiResponse<PeerFeedback>>('/performance/feedbacks', feedbackData),
    onSuccess: () => {
      toast.success('Peer feedback submitted successfully!');
      setShowFeedbackModal(false);
      resetFeedbackForm();
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to submit peer feedback');
    }
  });

  // Apply Normalization Mutation
  const applyNormalizationMutation = useMutation({
    mutationFn: () => api.post<ApiResponse<NormalizationPreview[]>>(`/performance/cycles/${currentCycleId}/normalize`),
    onSuccess: () => {
      toast.success('Bell-curve normalization applied successfully!');
      queryClient.invalidateQueries({ queryKey: ['normalization'] });
      refetchNormalization();
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to apply normalization');
    }
  });

  const resetGoalForm = () => {
    setGoalTitle('');
    setGoalDesc('');
    setGoalType('INDIVIDUAL');
    setGoalCategory('');
    setTargetVal(100);
    setCurrentVal(0);
    setWeight(10);
    setUnit('%');
    setStartDate('');
    setDueDate('');
  };

  const resetFeedbackForm = () => {
    setFeedbackToEmpId('');
    setFeedbackRating(4.0);
    setFeedbackStrengths('');
    setFeedbackImprovement('');
    setFeedbackComments('');
    setFeedbackAnonymous(false);
  };

  const handleEditGoal = (goal: Goal) => {
    setEditingGoal(goal);
    setGoalTitle(goal.title);
    setGoalDesc(goal.description);
    setGoalType(goal.type);
    setGoalCategory(goal.category);
    setTargetVal(Number(goal.targetValue));
    setCurrentVal(Number(goal.currentValue));
    setWeight(Number(goal.weight));
    setUnit(goal.unit);
    setStartDate(goal.startDate);
    setDueDate(goal.dueDate);
    setShowGoalModal(true);
  };

  const handleSaveGoal = (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentCycleId) {
      toast.error('No review cycle selected. Please select or create a cycle first.');
      return;
    }
    if (!goalTitle || !startDate || !dueDate) {
      toast.error('Please fill in all required fields: Title, Start Date, and Due Date.');
      return;
    }
    saveGoalMutation.mutate({
      employeeId: empId,
      reviewCycleId: currentCycleId,
      title: goalTitle,
      description: goalDesc,
      type: goalType,
      category: goalCategory || 'General',
      targetValue: targetVal,
      currentValue: currentVal,
      weight: weight,
      unit: unit,
      startDate: startDate,
      dueDate: dueDate,
      status: editingGoal ? editingGoal.status : 'PENDING'
    });
  };

  const handleOpenReviewModal = (review: PerformanceReview) => {
    setSelectedReview(review);
    setReviewGoalScore(Number(review.goalScore) || 3.0);
    setReviewCompScore(Number(review.competencyScore) || 3.0);
    setReviewStrengths(review.strengths || '');
    setReviewImprovement(review.improvementAreas || '');
    setReviewComments(review.reviewType === 'SELF' ? review.employeeComments || '' : review.managerComments || '');
    setShowReviewModal(true);
  };

  const handleSaveReview = (status: 'DRAFT' | 'SUBMITTED') => {
    if (!selectedReview) return;

    submitReviewMutation.mutate({
      employeeId: selectedReview.employeeId,
      reviewerId: empId,
      cycleId: selectedReview.cycleId,
      reviewType: selectedReview.reviewType,
      goalScore: reviewGoalScore,
      competencyScore: reviewCompScore,
      strengths: reviewStrengths,
      improvementAreas: reviewImprovement,
      employeeComments: selectedReview.reviewType === 'SELF' ? reviewComments : undefined,
      managerComments: selectedReview.reviewType !== 'SELF' ? reviewComments : undefined,
      status: status
    });
  };

  const handleSaveFeedback = (e: React.FormEvent) => {
    e.preventDefault();
    if (!feedbackToEmpId || !currentCycleId) {
      toast.error('Please select an employee.');
      return;
    }
    submitFeedbackMutation.mutate({
      reviewCycleId: currentCycleId,
      fromEmployeeId: empId,
      toEmployeeId: Number(feedbackToEmpId),
      rating: feedbackRating,
      strengths: feedbackStrengths,
      improvementAreas: feedbackImprovement,
      comments: feedbackComments,
      anonymous: feedbackAnonymous
    });
  };

  return (
    <div className="page-content">
      <div className="page-header">
        <div>
          <h1>Performance Management</h1>
          <p>OKR Goals, 360 Evaluations, and Rating Distributions</p>
        </div>
        <div className="header-actions">
          {cycles && cycles.length > 0 && (
            <select
              className="select cycle-selector"
              value={currentCycleId || ''}
              onChange={(e) => setSelectedCycleId(Number(e.target.value))}
            >
              {cycles.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.status})
                </option>
              ))}
            </select>
          )}
          {(hasRole('ROLE_ADMIN') || hasRole('ROLE_HR_MANAGER')) && (
            <button className="btn btn-secondary" onClick={() => setShowCycleModal(true)}>
              + Create Cycle
            </button>
          )}
          {activeTab === 'goals' && (
            <button className="btn btn-primary" onClick={() => { setEditingGoal(null); resetGoalForm(); setShowGoalModal(true); }}>
              <Plus size={16} /> Add OKR Goal
            </button>
          )}
          {activeTab === 'feedback' && (
            <button className="btn btn-primary" onClick={() => setShowFeedbackModal(true)}>
              <Star size={16} /> Submit Peer Feedback
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs">
        <button className={`tab ${activeTab === 'goals' ? 'active' : ''}`} onClick={() => setActiveTab('goals')}>
          <Target size={16} /> Goals & OKRs
        </button>
        <button className={`tab ${activeTab === 'reviews' ? 'active' : ''}`} onClick={() => setActiveTab('reviews')}>
          <Award size={16} /> Performance Reviews
        </button>
        <button className={`tab ${activeTab === 'feedback' ? 'active' : ''}`} onClick={() => setActiveTab('feedback')}>
          <Users size={16} /> Peer Feedback
        </button>
        {(hasRole('ROLE_ADMIN') || hasRole('ROLE_HR_MANAGER')) && (
          <button className={`tab ${activeTab === 'normalization' ? 'active' : ''}`} onClick={() => setActiveTab('normalization')}>
            <TrendingUp size={16} /> Rating Normalization
          </button>
        )}
      </div>

      {/* Goal Content */}
      {activeTab === 'goals' && (
        <div className="performance-section animate-fade">
          {isLoadingGoals ? (
            <div className="empty-state"><div className="spinner" /></div>
          ) : !goals || goals.length === 0 ? (
            <div className="card empty-state">
              <Target size={48} />
              <h3>No goals set for this cycle</h3>
              <p>Goals help define OKRs and SMART tracking. Create your first goal to get started.</p>
              <button className="btn btn-primary" onClick={() => setShowGoalModal(true)}>
                <Plus size={16} /> Create Goal
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-2">
              {goals.map((goal) => {
                const calculatedProgress = goal.progress ?? Math.min(100, Math.round((Number(goal.currentValue) / Number(goal.targetValue)) * 100));
                return (
                  <div key={goal.id} className="card goal-card">
                    <div className="goal-card-header">
                      <span className={`badge badge-goal-type ${goal.type.toLowerCase()}`}>{goal.type}</span>
                      <span className={`badge badge-goal-status ${goal.status.toLowerCase().replace('_', '-')}`}>
                        {goal.status.replace('_', ' ')}
                      </span>
                    </div>
                    <h3 className="goal-title">{goal.title}</h3>
                    <p className="goal-desc">{goal.description}</p>

                    <div className="goal-metrics">
                      <div className="metric">
                        <span className="label">Target:</span>
                        <span className="value">{Number(goal.targetValue)} {goal.unit}</span>
                      </div>
                      <div className="metric">
                        <span className="label">Current:</span>
                        <span className="value">{Number(goal.currentValue)} {goal.unit}</span>
                      </div>
                      <div className="metric">
                        <span className="label">Weight:</span>
                        <span className="value">{Number(goal.weight)}%</span>
                      </div>
                    </div>

                    <div className="goal-progress-section">
                      <div className="progress-text">
                        <span>Progress</span>
                        <span>{calculatedProgress}%</span>
                      </div>
                      <div className="progress-bar-container">
                        <div className="progress-bar" style={{ width: `${calculatedProgress}%` }} />
                      </div>
                      <input
                        type="range"
                        min="0"
                        max="100"
                        value={calculatedProgress}
                        onChange={(e) => updateGoalProgressMutation.mutate({ goalId: goal.id, progress: Number(e.target.value) })}
                        className="progress-slider"
                      />
                    </div>

                    <div className="goal-card-actions">
                      <button className="btn-ghost btn-sm" onClick={() => handleEditGoal(goal)}>
                        <Edit2 size={14} /> Edit details
                      </button>
                      <div className="goal-dates">
                        Due: {new Date(goal.dueDate).toLocaleDateString()}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Reviews Content */}
      {activeTab === 'reviews' && (
        <div className="performance-section animate-fade">
          {isLoadingReviews ? (
            <div className="empty-state"><div className="spinner" /></div>
          ) : !myReviews || myReviews.length === 0 ? (
            <div className="card empty-state">
              <AwardIcon size={48} />
              <h3>No reviews found</h3>
              <p>You can start your self evaluation if a cycle is active.</p>
              {currentCycleId && (
                <button
                  className="btn btn-primary"
                  style={{ marginTop: '1rem' }}
                  onClick={() => {
                    handleOpenReviewModal({
                      id: 0,
                      employeeId: empId,
                      employeeName: user?.fullName || 'Me',
                      reviewerId: empId,
                      reviewerName: user?.fullName || 'Me',
                      cycleId: currentCycleId,
                      cycleName: cycles?.find(c => c.id === currentCycleId)?.name || 'Current Cycle',
                      reviewType: 'SELF',
                      status: 'DRAFT'
                    } as any);
                  }}
                >
                  Start Self Evaluation
                </button>
              )}
            </div>
          ) : (
            <div className="reviews-list-container">
              {currentCycleId && !myReviews.some(r => r.reviewType === 'SELF' && r.cycleId === currentCycleId) && (
                <div style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
                  <button
                    className="btn btn-primary btn-sm"
                    onClick={() => {
                      handleOpenReviewModal({
                        id: 0,
                        employeeId: empId,
                        employeeName: user?.fullName || 'Me',
                        reviewerId: empId,
                        reviewerName: user?.fullName || 'Me',
                        cycleId: currentCycleId,
                        cycleName: cycles?.find(c => c.id === currentCycleId)?.name || 'Current Cycle',
                        reviewType: 'SELF',
                        status: 'DRAFT'
                      } as any);
                    }}
                  >
                    + Start Self Evaluation
                  </button>
                </div>
              )}
              <div className="reviews-list">
                {myReviews.map((rev) => (
                <div key={rev.id} className="card review-item-card">
                  <div className="review-meta">
                    <div>
                      <span className="badge badge-review-type">{rev.reviewType} Review</span>
                      <span className={`badge badge-review-status ${rev.status.toLowerCase()}`}>{rev.status}</span>
                    </div>
                    <span className="review-cycle-title">{rev.cycleName}</span>
                  </div>

                  <div className="review-details-grid">
                    <div className="detail-col">
                      <div className="detail-label">Employee</div>
                      <div className="detail-val">{rev.employeeName}</div>
                    </div>
                    <div className="detail-col">
                      <div className="detail-label">Reviewer</div>
                      <div className="detail-val">{rev.reviewerName || 'Unassigned'}</div>
                    </div>
                    <div className="detail-col">
                      <div className="detail-label">Final Score</div>
                      <div className="detail-val highlight">{rev.finalScore ? Number(rev.finalScore).toFixed(1) : 'Pending'}</div>
                    </div>
                    <div className="detail-col">
                      <div className="detail-label">Rating Band</div>
                      <div className="detail-val highlight">{rev.band || 'Pending'}</div>
                    </div>
                  </div>

                  <div className="review-actions">
                    <button className="btn btn-secondary" onClick={() => handleOpenReviewModal(rev)}>
                      {rev.status === 'DRAFT' ? 'Fill Evaluation' : 'View Details'}
                    </button>
                    {rev.status === 'SUBMITTED' && rev.reviewerId !== empId && (
                      <button
                        className="btn btn-primary"
                        onClick={async () => {
                          await api.post(`/performance/reviews/${rev.id}/acknowledge`);
                          toast.success('Review acknowledged!');
                          queryClient.invalidateQueries({ queryKey: ['myReviews'] });
                        }}
                      >
                        <CheckCircle size={14} /> Acknowledge Review
                      </button>
                    )}
                  </div>
                </div>
              ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Feedback Content */}
      {activeTab === 'feedback' && (
        <div className="performance-section animate-fade">
          <div className="feedback-layout">
            <div className="card feedback-received-card">
              <h3><Users size={18} /> Peer Feedbacks Received</h3>
              {!feedbackForMe || feedbackForMe.length === 0 ? (
                <div className="empty-state">
                  <Star size={36} />
                  <p>No feedback received yet in this cycle</p>
                </div>
              ) : (
                <div className="feedback-list">
                  {feedbackForMe.map((fb) => (
                    <div key={fb.id} className="feedback-card-item">
                      <div className="fb-header">
                        <span className="fb-from">{fb.anonymous ? 'Anonymous Colleague' : fb.fromEmployeeName}</span>
                        <div className="fb-rating">
                          <Star size={12} className="star-filled" /> {Number(fb.rating).toFixed(1)}
                        </div>
                      </div>
                      <div className="fb-text">
                        <strong>Strengths:</strong> {fb.strengths}
                      </div>
                      <div className="fb-text">
                        <strong>Improvement Areas:</strong> {fb.improvementAreas}
                      </div>
                      {fb.comments && (
                        <div className="fb-comments">
                          "{fb.comments}"
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="card feedback-guidelines">
              <h3>360 Feedback Guidelines</h3>
              <p>Giving constructive feedback helps coworkers grow. Focus on:</p>
              <ul>
                <li><strong>Actionable insights:</strong> Suggest specific ways to improve.</li>
                <li><strong>Objectivity:</strong> Avoid personal bias and focus on work outcomes.</li>
                <li><strong>Encouragement:</strong> Recognize strengths and milestones.</li>
              </ul>
              <button className="btn btn-primary" style={{ marginTop: '1rem', width: '100%' }} onClick={() => setShowFeedbackModal(true)}>
                Submit New Feedback
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Normalization Content */}
      {activeTab === 'normalization' && (
        <div className="performance-section animate-fade">
          <div className="card normalization-dashboard">
            <div className="normalization-header">
              <div>
                <h3>Bell-Curve Rating Normalization</h3>
                <p>Ensuring balanced employee rating distributions across the company.</p>
              </div>
              <button
                className="btn btn-primary"
                onClick={() => applyNormalizationMutation.mutate()}
                disabled={applyNormalizationMutation.isPending || !normalizationData || normalizationData.length === 0}
              >
                <Check size={16} /> Apply Bell-Curve Normalization
              </button>
            </div>

            {isLoadingNormalization ? (
              <div className="empty-state"><div className="spinner" /></div>
            ) : !normalizationData || normalizationData.length === 0 ? (
              <div className="empty-state">
                <ShieldAlert size={36} />
                <p>No active evaluations to normalize in this cycle.</p>
              </div>
            ) : (
              <div className="table-wrapper">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Employee</th>
                      <th>Department</th>
                      <th>Raw Score</th>
                      <th>Current Band</th>
                      <th>Suggested normalized Band</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {normalizationData.map((row) => (
                      <tr key={row.reviewId}>
                        <td>{row.employeeName}</td>
                        <td>{row.departmentName}</td>
                        <td>{Number(row.finalScore).toFixed(2)}</td>
                        <td><span className="badge badge-normal-current">{row.currentBand || 'N/A'}</span></td>
                        <td><span className="badge badge-normal-suggested">{row.suggestedBand}</span></td>
                        <td>
                          {row.currentBand === row.suggestedBand ? (
                            <span className="status-no-change">Matched</span>
                          ) : (
                            <span className="status-adjusted">Adjusted</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Add/Edit Goal Modal */}
      {showGoalModal && (
        <div className="modal-overlay">
          <div className="modal-content card">
            <h2>{editingGoal ? 'Edit Goal' : 'Create SMART Goal'}</h2>
            <form onSubmit={handleSaveGoal}>
              <div className="grid grid-cols-2">
                <div>
                  <label className="form-label">Goal Title *</label>
                  <input type="text" className="input" value={goalTitle} onChange={(e) => setGoalTitle(e.target.value)} required />
                </div>
                <div>
                  <label className="form-label">Type</label>
                  <select className="select" value={goalType} onChange={(e: any) => setGoalType(e.target.value)}>
                    <option value="INDIVIDUAL">Individual</option>
                    <option value="DEPARTMENT">Department</option>
                    <option value="COMPANY">Company</option>
                  </select>
                </div>
                <div>
                  <label className="form-label">Category</label>
                  <input type="text" className="input" placeholder="e.g. Sales, Coding" value={goalCategory} onChange={(e) => setGoalCategory(e.target.value)} />
                </div>
                <div>
                  <label className="form-label">Weight (%)</label>
                  <input type="number" className="input" value={weight} onChange={(e) => setWeight(Number(e.target.value))} min="1" max="100" />
                </div>
                <div>
                  <label className="form-label">Target Value</label>
                  <input type="number" className="input" value={targetVal} onChange={(e) => setTargetVal(Number(e.target.value))} />
                </div>
                <div>
                  <label className="form-label">Unit</label>
                  <input type="text" className="input" value={unit} onChange={(e) => setUnit(e.target.value)} />
                </div>
                <div>
                  <label className="form-label">Start Date *</label>
                  <input type="date" className="input" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
                </div>
                <div>
                  <label className="form-label">Due Date *</label>
                  <input type="date" className="input" value={dueDate} onChange={(e) => setDueDate(e.target.value)} required />
                </div>
              </div>
              <div style={{ marginTop: '1rem' }}>
                <label className="form-label">Goal Description</label>
                <textarea className="textarea" value={goalDesc} onChange={(e) => setGoalDesc(e.target.value)} />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowGoalModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saveGoalMutation.isPending}>Save Goal</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Review Dialog Modal */}
      {showReviewModal && selectedReview && (
        <div className="modal-overlay">
          <div className="modal-content card">
            <h2>{selectedReview.reviewType} Performance Review</h2>
            <p className="subtitle">{selectedReview.employeeName} · {selectedReview.employeeDesignation}</p>

            <form onSubmit={(e) => e.preventDefault()}>
              <div className="grid grid-cols-2" style={{ marginTop: '1rem' }}>
                <div>
                  <label className="form-label">Goal Achievement Score (1.0 - 5.0)</label>
                  <input
                    type="number"
                    step="0.1"
                    min="1"
                    max="5"
                    className="input"
                    value={reviewGoalScore}
                    onChange={(e) => setReviewGoalScore(Number(e.target.value))}
                    disabled={selectedReview.status !== 'DRAFT'}
                  />
                </div>
                <div>
                  <label className="form-label">Core Competency Score (1.0 - 5.0)</label>
                  <input
                    type="number"
                    step="0.1"
                    min="1"
                    max="5"
                    className="input"
                    value={reviewCompScore}
                    onChange={(e) => setReviewCompScore(Number(e.target.value))}
                    disabled={selectedReview.status !== 'DRAFT'}
                  />
                </div>
              </div>

              <div style={{ marginTop: '1rem' }}>
                <label className="form-label">Strengths</label>
                <textarea
                  className="textarea"
                  value={reviewStrengths}
                  onChange={(e) => setReviewStrengths(e.target.value)}
                  placeholder="Identify areas of excellence..."
                  disabled={selectedReview.status !== 'DRAFT'}
                />
              </div>

              <div style={{ marginTop: '1rem' }}>
                <label className="form-label">Areas for Improvement</label>
                <textarea
                  className="textarea"
                  value={reviewImprovement}
                  onChange={(e) => setReviewImprovement(e.target.value)}
                  placeholder="Key development suggestions..."
                  disabled={selectedReview.status !== 'DRAFT'}
                />
              </div>

              <div style={{ marginTop: '1rem' }}>
                <label className="form-label">Comments</label>
                <textarea
                  className="textarea"
                  value={reviewComments}
                  onChange={(e) => setReviewComments(e.target.value)}
                  placeholder="Additional remarks..."
                  disabled={selectedReview.status !== 'DRAFT'}
                />
              </div>

              <div className="modal-actions" style={{ marginTop: '1.5rem', display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-ghost" onClick={() => setShowReviewModal(false)}>Cancel</button>
                {selectedReview.status === 'DRAFT' && (
                  <>
                    <button type="button" className="btn btn-secondary" disabled={submitReviewMutation.isPending} onClick={() => handleSaveReview('DRAFT')}>Save Draft</button>
                    <button type="button" className="btn btn-primary" disabled={submitReviewMutation.isPending} onClick={() => handleSaveReview('SUBMITTED')}>Submit Evaluation</button>
                  </>
                )}
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Peer Feedback Submission Modal */}
      {showFeedbackModal && (
        <div className="modal-overlay">
          <div className="modal-content card">
            <h2>Submit Peer Feedback</h2>
            <p className="subtitle">Provide feedback for a colleague's professional growth.</p>
            <form onSubmit={handleSaveFeedback}>
              <div style={{ marginTop: '1rem' }}>
                <label className="form-label">Select Employee *</label>
                <input
                  type="number"
                  className="input"
                  placeholder="Enter employee ID..."
                  value={feedbackToEmpId}
                  onChange={(e) => setFeedbackToEmpId(e.target.value)}
                  required
                />
              </div>

              <div style={{ marginTop: '1rem' }}>
                <label className="form-label">Overall Rating (1.0 - 5.0)</label>
                <input
                  type="number"
                  step="0.1"
                  min="1"
                  max="5"
                  className="input"
                  value={feedbackRating}
                  onChange={(e) => setFeedbackRating(Number(e.target.value))}
                  required
                />
              </div>

              <div style={{ marginTop: '1rem' }}>
                <label className="form-label">Strengths observed</label>
                <textarea
                  className="textarea"
                  value={feedbackStrengths}
                  onChange={(e) => setFeedbackStrengths(e.target.value)}
                  placeholder="What does this person do well?"
                />
              </div>

              <div style={{ marginTop: '1rem' }}>
                <label className="form-label">Constructive suggestions</label>
                <textarea
                  className="textarea"
                  value={feedbackImprovement}
                  onChange={(e) => setFeedbackImprovement(e.target.value)}
                  placeholder="How could they improve?"
                />
              </div>

              <div style={{ marginTop: '1rem' }}>
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={feedbackAnonymous}
                    onChange={(e) => setFeedbackAnonymous(e.target.checked)}
                  />
                  Submit Anonymously
                </label>
              </div>

              <div className="modal-actions" style={{ marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowFeedbackModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={submitFeedbackMutation.isPending}>Submit Feedback</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Create Cycle Modal */}
      {showCycleModal && (
        <div className="modal-overlay">
          <div className="modal-content card">
            <h2>Create Review Cycle</h2>
            <p className="subtitle">Initialize a new performance review period for the company.</p>
            <form onSubmit={handleCreateCycle}>
              <div style={{ marginTop: '1rem' }}>
                <label className="form-label">Cycle Name *</label>
                <input
                  type="text"
                  className="input"
                  placeholder="e.g. Q3 2026 Performance Review"
                  value={cycleName}
                  onChange={(e) => setCycleName(e.target.value)}
                  required
                />
              </div>
              <div className="grid grid-cols-2" style={{ marginTop: '1rem' }}>
                <div>
                  <label className="form-label">Start Date *</label>
                  <input
                    type="date"
                    className="input"
                    value={cycleStartDate}
                    onChange={(e) => setCycleStartDate(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className="form-label">End Date *</label>
                  <input
                    type="date"
                    className="input"
                    value={cycleEndDate}
                    onChange={(e) => setCycleEndDate(e.target.value)}
                    required
                  />
                </div>
              </div>
              <div className="modal-actions" style={{ marginTop: '1.5rem', display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-ghost" onClick={() => setShowCycleModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={createCycleMutation.isPending}>
                  {createCycleMutation.isPending ? 'Creating...' : 'Create Cycle'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
