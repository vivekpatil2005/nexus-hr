import { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../lib/auth';
import api from '../lib/api';
import type { AttritionPrediction, SkillGap, ChatResponse, ApiResponse, EmployeeSummary, PagedResponse } from '../lib/types';
import {
  Brain, Bot, Send, RefreshCw, AlertTriangle, TrendingDown,
  Compass, Award, Sparkles, ShieldAlert,
  CheckCircle, CheckSquare, Square
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import toast from 'react-hot-toast';
import './AiPage.css';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  source?: string;
  timestamp: Date;
}

export default function AiPage() {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();
  const chatEndRef = useRef<HTMLDivElement>(null);

  // Tab State
  const isAdminOrManager = hasRole('ROLE_ADMIN') || hasRole('ROLE_HR_MANAGER') || hasRole('ROLE_MANAGER');
  const [activeTab, setActiveTab] = useState<'chatbot' | 'skills' | 'attrition'>(
    isAdminOrManager ? 'attrition' : 'chatbot'
  );

  // Chatbot State
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 'welcome',
      role: 'assistant',
      content: 'Hello! I am **NexusHR Copilot**, your AI HR assistant.\n\nI can help you with queries about:\n- **Leave Policies** (e.g. "How many sick leaves do I get?")\n- **Payroll & Payslips** (e.g. "When is the salary cycle?")\n- **Performance Cycles** (e.g. "How are goals tracked?")\n- **AI Analytics** (e.g. "How is the attrition risk computed?")\n\nPlease ask a question about any of these areas!',
      source: 'LOCAL_KNOWLEDGE_BASE',
      timestamp: new Date()
    }
  ]);
  const [chatInput, setChatInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  // Skill Gap State
  const [selectedEmpId, setSelectedEmpId] = useState<string>('');
  const [completedRecommendations, setCompletedRecommendations] = useState<Record<string, boolean>>({});

  const myEmpId = String(user?.employeeId || user?.id || '');

  // Fetch employees for select box (only if admin/manager)
  const { data: employeesData } = useQuery<PagedResponse<EmployeeSummary>>({
    queryKey: ['aiEmployeesList'],
    queryFn: () => api.get<ApiResponse<PagedResponse<EmployeeSummary>>>('/employees?size=200').then(r => r.data.data),
    enabled: isAdminOrManager,
  });

  // Default selected employee to logged-in user or first employee
  useEffect(() => {
    if (!selectedEmpId) {
      if (isAdminOrManager && employeesData?.content?.length) {
        setSelectedEmpId(String(employeesData.content[0].id));
      } else if (myEmpId) {
        setSelectedEmpId(myEmpId);
      }
    }
  }, [employeesData, isAdminOrManager, myEmpId, selectedEmpId]);

  // Fetch attrition scores
  const { data: attritionScores, isLoading: isLoadingAttrition } = useQuery<AttritionPrediction[]>({
    queryKey: ['attritionScores'],
    queryFn: () => api.get<ApiResponse<AttritionPrediction[]>>('/ai/attrition').then(r => r.data.data),
  });

  // Fetch skill gap analysis
  const { data: skillGapData, isLoading: isLoadingSkillGap } = useQuery<SkillGap>({
    queryKey: ['skillGap', selectedEmpId],
    queryFn: () => api.get<ApiResponse<SkillGap>>(`/ai/skills/gap/${selectedEmpId}`).then(r => r.data.data),
    enabled: !!selectedEmpId,
  });

  // Attrition Mutations
  const recomputeAllMutation = useMutation({
    mutationFn: () => api.post<ApiResponse<string>>('/ai/attrition/compute'),
    onSuccess: () => {
      toast.success('Workforce attrition matrix recomputed!');
      queryClient.invalidateQueries({ queryKey: ['attritionScores'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Recomputation failed');
    }
  });

  const recomputeSingleMutation = useMutation({
    mutationFn: (empId: number) => api.post<ApiResponse<AttritionPrediction>>(`/ai/attrition/compute/${empId}`),
    onSuccess: () => {
      toast.success('Employee stability factor updated!');
      queryClient.invalidateQueries({ queryKey: ['attritionScores'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Single recomputation failed');
    }
  });

  // Chatbot Send Message
  const sendChatMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim()) return;

    const userMsg: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: chatInput,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMsg]);
    const prompt = chatInput;
    setChatInput('');
    setIsTyping(true);

    try {
      const { data } = await api.post<ApiResponse<ChatResponse>>('/ai/chat', { message: prompt });
      const reply = data.data;

      setMessages(prev => [...prev, {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: reply.response,
        source: reply.source,
        timestamp: new Date()
      }]);
    } catch (err: any) {
      setMessages(prev => [...prev, {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: 'I encountered an error connecting to the AI brain. Please try again shortly.',
        source: 'ERROR_FALLBACK',
        timestamp: new Date()
      }]);
    } finally {
      setIsTyping(false);
    }
  };

  // Scroll to bottom of chat
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  // Parse custom Markdown bold & structure helper
  const parseBold = (text: string) => {
    const parts = text.split(/(\*\*.*?\*\*)/g);
    return parts.map((part, i) => {
      if (part.startsWith('**') && part.endsWith('**')) {
        return <strong key={i} className="font-semibold text-white">{part.slice(2, -2)}</strong>;
      }
      return part;
    });
  };

  const renderMarkdown = (text: string) => {
    if (!text) return '';
    return text.split('\n').map((line, index) => {
      if (line.startsWith('### ')) {
        return <h4 key={index} className="chat-h4">{line.substring(4)}</h4>;
      }
      if (line.startsWith('## ')) {
        return <h3 key={index} className="chat-h3">{line.substring(3)}</h3>;
      }
      if (line.startsWith('# ')) {
        return <h2 key={index} className="chat-h2">{line.substring(2)}</h2>;
      }
      if (line.startsWith('- ') || line.startsWith('* ')) {
        return (
          <li key={index} className="chat-li">
            {parseBold(line.substring(2))}
          </li>
        );
      }
      if (/^\d+\.\s/.test(line)) {
        const content = line.replace(/^\d+\.\s/, '');
        return (
          <li key={index} className="chat-li-numbered">
            {parseBold(content)}
          </li>
        );
      }
      return <p key={index} className="chat-p">{parseBold(line)}</p>;
    });
  };

  // Render Risk Level Badge
  const getRiskBadge = (level: string) => {
    const lvl = level?.toUpperCase() || 'LOW';
    if (lvl === 'HIGH' || lvl === 'CRITICAL') {
      return <span className="badge badge-danger"><AlertTriangle size={12} /> {lvl} RISK</span>;
    }
    if (lvl === 'MEDIUM') {
      return <span className="badge badge-warning">{lvl} RISK</span>;
    }
    return <span className="badge badge-success">{lvl} RISK</span>;
  };

  // Stats computation for Attrition
  const totalScored = attritionScores?.length || 0;
  const highRiskCount = attritionScores?.filter(s => s.riskLevel === 'HIGH' || s.riskLevel === 'CRITICAL').length || 0;
  const mediumRiskCount = attritionScores?.filter(s => s.riskLevel === 'MEDIUM').length || 0;
  const lowRiskCount = attritionScores?.filter(s => s.riskLevel === 'LOW').length || 0;



  // Skill Gap Chart Data
  const chartData = skillGapData
    ? Object.keys(skillGapData.requiredSkills).map(skill => ({
        skill,
        Current: skillGapData.currentSkills[skill] || 0,
        Required: skillGapData.requiredSkills[skill] || 0,
      }))
    : [];

  return (
    <div className="page-content">
      <div className="page-header">
        <div>
          <h1>AI Workforce Intelligence</h1>
          <p>Predictive analytics, upskilling recommendations, and AI assistant</p>
        </div>
        <div className="header-actions">
          {activeTab === 'attrition' && isAdminOrManager && (
            <button
              className="btn btn-primary"
              onClick={() => recomputeAllMutation.mutate()}
              disabled={recomputeAllMutation.isPending}
            >
              <RefreshCw size={16} className={recomputeAllMutation.isPending ? 'spin' : ''} />
              Recompute Scores
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs">
        {isAdminOrManager && (
          <button
            className={`tab ${activeTab === 'attrition' ? 'active' : ''}`}
            onClick={() => setActiveTab('attrition')}
          >
            <TrendingDown size={16} /> Attrition Risk Matrix
          </button>
        )}
        <button
          className={`tab ${activeTab === 'skills' ? 'active' : ''}`}
          onClick={() => setActiveTab('skills')}
        >
          <Award size={16} /> Skill Gap Analytics
        </button>
        <button
          className={`tab ${activeTab === 'chatbot' ? 'active' : ''}`}
          onClick={() => setActiveTab('chatbot')}
        >
          <Bot size={16} /> AI HR Copilot
        </button>
      </div>

      {/* 1. Attrition Risk Matrix Content (Admins/Managers) */}
      {activeTab === 'attrition' && isAdminOrManager && (
        <div className="ai-section animate-fade">
          {/* Summary Cards */}
          <div className="grid grid-cols-4 ai-stats">
            <div className="stat-card">
              <div className="stat-icon purple"><Brain size={22} /></div>
              <div className="stat-info">
                <div className="stat-value">{totalScored}</div>
                <div className="stat-label">Total Monitored</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon red"><AlertTriangle size={22} /></div>
              <div className="stat-info">
                <div className="stat-value">{highRiskCount}</div>
                <div className="stat-label">High/Critical Risk</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon amber"><TrendingDown size={22} /></div>
              <div className="stat-info">
                <div className="stat-value">{mediumRiskCount}</div>
                <div className="stat-label">Medium Risk</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon green"><CheckCircle size={22} /></div>
              <div className="stat-info">
                <div className="stat-value">{lowRiskCount}</div>
                <div className="stat-label">Low Risk/Stable</div>
              </div>
            </div>
          </div>

          {/* Attrition Table */}
          <div className="card table-card">
            <div className="card-header">
              <h3>Active Employee Retention Ratios</h3>
              <span className="text-muted">Calculated daily using machine learning heuristics</span>
            </div>

            {isLoadingAttrition ? (
              <div className="empty-state"><div className="spinner" /></div>
            ) : !attritionScores || attritionScores.length === 0 ? (
              <div className="empty-state">
                <ShieldAlert size={48} />
                <h3>No Attrition Scores Available</h3>
                <p>Click "Recompute Scores" to run the evaluation algorithms.</p>
              </div>
            ) : (
              <div className="table-wrapper">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Employee</th>
                      <th>Department</th>
                      <th>Risk Level</th>
                      <th>Retention Probability</th>
                      <th>Risk Drivers & Features</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {attritionScores.map((score) => {
                      const retentionPct = Math.round((1 - score.score) * 100);
                      const features = score.features || {};
                      
                      return (
                        <tr key={score.id}>
                          <td>
                            <div className="employee-info-cell">
                              <span className="emp-name">{score.employeeName}</span>
                              <span className="emp-id-label">ID: {score.employeeId}</span>
                            </div>
                          </td>
                          <td>{score.departmentName}</td>
                          <td>{getRiskBadge(score.riskLevel)}</td>
                          <td>
                            <div className="retention-pct-cell">
                              <span className="pct-value">{retentionPct}%</span>
                              <div className="pct-bar-bg">
                                <div 
                                  className={`pct-bar ${retentionPct < 40 ? 'red' : retentionPct < 70 ? 'amber' : 'green'}`}
                                  style={{ width: `${retentionPct}%` }}
                                />
                              </div>
                            </div>
                          </td>
                          <td>
                            <div className="feature-tags-container">
                              {features.tenure_risk && <span className="feature-tag red">Tenure &lt; 1yr</span>}
                              {features.low_salary_risk && <span className="feature-tag red">Low Salary Percentile</span>}
                              {features.performance_risk && <span className="feature-tag red">Low Performance</span>}
                              {features.performance_drop && <span className="feature-tag red">Perf. Decline</span>}
                              {features.excessive_leave_risk && <span className="feature-tag amber">Excessive Leaves</span>}
                              {features.no_manager_risk && <span className="feature-tag amber">No Manager</span>}
                              {!features.tenure_risk && !features.low_salary_risk && !features.performance_risk && !features.performance_drop && !features.excessive_leave_risk && !features.no_manager_risk && (
                                <span className="feature-tag green">No risk factors identified</span>
                              )}
                            </div>
                          </td>
                          <td>
                            <button
                              className="btn btn-secondary btn-sm"
                              onClick={() => recomputeSingleMutation.mutate(Number(score.employeeId))}
                              disabled={recomputeSingleMutation.isPending}
                            >
                              <RefreshCw size={12} />
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 2. Skill Gap Analytics Content */}
      {activeTab === 'skills' && (
        <div className="ai-section animate-fade">
          <div className="skills-layout-grid">
            {/* Left side controller */}
            <div className="card skills-selector-card">
              <h3><Compass size={18} /> Skill Gap Assessment</h3>
              <p className="text-muted">Compare actual skill weights against department requirements.</p>

              {isAdminOrManager ? (
                <div style={{ marginTop: '1.5rem' }}>
                  <label className="form-label">Analyze Employee Profile</label>
                  <select
                    className="select"
                    value={selectedEmpId}
                    onChange={(e) => setSelectedEmpId(e.target.value)}
                  >
                    <option value="">Select Employee...</option>
                    {employeesData?.content?.map(emp => (
                      <option key={emp.id} value={emp.id}>
                        {emp.fullName} ({emp.designation})
                      </option>
                    ))}
                  </select>
                </div>
              ) : (
                <div className="locked-employee-info">
                  <div className="avatar">{user?.fullName.split(' ').map(n => n[0]).join('').substring(0, 2)}</div>
                  <div>
                    <h4 className="name">{user?.fullName}</h4>
                    <span className="role">{user?.roles?.[0]?.replace('ROLE_', '')}</span>
                  </div>
                </div>
              )}

              {/* Show simple stats */}
              {skillGapData && (
                <div className="skill-summary-stats" style={{ marginTop: '2rem' }}>
                  <div className="stat-row">
                    <span className="lbl">Designation Profile</span>
                    <span className="val">{skillGapData.designation}</span>
                  </div>
                  <div className="stat-row">
                    <span className="lbl">Skills Tracked</span>
                    <span className="val">{Object.keys(skillGapData.requiredSkills).length}</span>
                  </div>
                  <div className="stat-row">
                    <span className="lbl">Core Gaps Found</span>
                    <span className="val">
                      {Object.values(skillGapData.gaps).filter(v => v > 0).length}
                    </span>
                  </div>
                </div>
              )}
            </div>

            {/* Right side analytics */}
            <div className="card skills-graph-card">
              {isLoadingSkillGap ? (
                <div className="empty-state"><div className="spinner" /></div>
              ) : !skillGapData ? (
                <div className="empty-state">
                  <Compass size={48} />
                  <h3>No Employee Selected</h3>
                  <p>Please select an employee profile to analyze competencies.</p>
                </div>
              ) : (
                <div>
                  <div className="graph-header">
                    <div>
                      <h3>{skillGapData.employeeName} · Core Skill Gap Profile</h3>
                      <p className="text-muted">Target rating (1-5 scale) vs current evaluated score</p>
                    </div>
                  </div>

                  {/* Recharts chart */}
                  <div className="chart-container" style={{ width: '100%', height: 300, marginTop: '1.5rem' }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={chartData} margin={{ top: 20, right: 20, left: -20, bottom: 5 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#1e1e22" vertical={false} />
                        <XAxis dataKey="skill" tick={{ fill: '#71717a', fontSize: 11 }} tickLine={false} />
                        <YAxis domain={[0, 5]} tick={{ fill: '#71717a', fontSize: 11 }} tickLine={false} />
                        <Tooltip
                          contentStyle={{
                            background: '#18181b',
                            border: '1px solid #27272a',
                            borderRadius: '8px',
                            fontSize: '0.8rem',
                            color: '#fafafa',
                          }}
                        />
                        <Legend wrapperStyle={{ fontSize: '0.8rem', marginTop: '10px' }} />
                        <Bar dataKey="Current" fill="#7c3aed" radius={[4, 4, 0, 0]} name="Current Level" />
                        <Bar dataKey="Required" fill="#3f3f46" radius={[4, 4, 0, 0]} name="Benchmark Requirement" />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>

                  {/* Recommendation check list */}
                  <div className="recommendations-container" style={{ marginTop: '2.5rem' }}>
                    <h3 className="section-title"><Sparkles size={16} /> AI Upskilling Action Plan</h3>
                    <p className="text-muted">Personalized learning modules to eliminate identified skill gaps</p>
                    
                    <div className="recommendation-list">
                      {skillGapData.recommendations.map((rec, index) => {
                        const isCompleted = completedRecommendations[`${selectedEmpId}-${index}`];
                        return (
                          <div 
                            key={index} 
                            className={`recommendation-item ${isCompleted ? 'completed' : ''}`}
                            onClick={() => {
                              setCompletedRecommendations(prev => ({
                                ...prev,
                                [`${selectedEmpId}-${index}`]: !prev[`${selectedEmpId}-${index}`]
                              }));
                            }}
                          >
                            <button className="btn-check">
                              {isCompleted ? (
                                <CheckSquare size={18} className="check-icon" />
                              ) : (
                                <Square size={18} className="uncheck-icon" />
                              )}
                            </button>
                            <span>{rec}</span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 3. AI HR Copilot Chatbot Content */}
      {activeTab === 'chatbot' && (
        <div className="ai-section chatbot-container animate-fade">
          <div className="card chat-card-wrapper">
            <div className="chat-header">
              <div className="bot-avatar"><Bot size={22} /></div>
              <div>
                <h3>NexusHR Copilot</h3>
                <span className="status-label">Online · AI Knowledge Sync</span>
              </div>
            </div>

            {/* Message Area */}
            <div className="chat-messages-area">
              {messages.map((msg) => (
                <div key={msg.id} className={`chat-message ${msg.role}`}>
                  <div className="message-bubble">
                    <div className="message-content">
                      {renderMarkdown(msg.content)}
                    </div>
                    {msg.source && (
                      <div className="message-meta">
                        Source: <span className="source-badge">{msg.source}</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
              {isTyping && (
                <div className="chat-message assistant">
                  <div className="message-bubble typing-bubble">
                    <div className="typing-indicator">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  </div>
                </div>
              )}
              <div ref={chatEndRef} />
            </div>

            {/* Input Form */}
            <form onSubmit={sendChatMessage} className="chat-input-form">
              <input
                type="text"
                className="input chat-input"
                placeholder="Ask about policies, leave rules, salary structures..."
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                disabled={isTyping}
              />
              <button
                type="submit"
                className="btn btn-primary chat-send-btn"
                disabled={isTyping || !chatInput.trim()}
              >
                <Send size={16} />
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
