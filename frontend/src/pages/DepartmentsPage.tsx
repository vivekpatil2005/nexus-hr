import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../lib/api';
import type { Department } from '../lib/types';
import { Building2, Users, ChevronRight, ChevronDown } from 'lucide-react';
import './DepartmentsPage.css';

const cardColors = ['#7c3aed', '#6366f1', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#06b6d4', '#14b8a6', '#f97316', '#a855f7', '#8b5cf6'];

export default function DepartmentsPage() {
  const { data: departments, isLoading } = useQuery({
    queryKey: ['departments'],
    queryFn: () => api.get<{ data: Department[] }>('/departments').then(r => r.data.data),
  });

  const { data: tree } = useQuery({
    queryKey: ['departments-tree'],
    queryFn: () => api.get<{ data: Department[] }>('/departments/tree').then(r => r.data.data),
  });

  return (
    <div className="page-content">
      <div className="page-header">
        <div>
          <h1>Departments</h1>
          <p>{departments?.length ?? 0} departments</p>
        </div>
      </div>

      {/* Department cards grid */}
      <div className="grid grid-cols-3 dept-grid">
        {isLoading
          ? Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="card dept-card">
                <div className="skeleton" style={{ height: 20, width: '60%', marginBottom: 8 }} />
                <div className="skeleton" style={{ height: 14, width: '80%' }} />
              </div>
            ))
          : departments?.map((dept, i) => (
              <div
                key={dept.id}
                className="card dept-card"
                style={{ borderTopColor: cardColors[i % cardColors.length] }}
              >
                <div className="dept-card-header">
                  <Building2 size={18} style={{ color: cardColors[i % cardColors.length] }} />
                  <h3>{dept.name}</h3>
                </div>
                {dept.description && (
                  <p className="dept-description">{dept.description}</p>
                )}
                <div className="dept-card-footer">
                  <div className="dept-stat">
                    <Users size={14} />
                    <span>{dept.employeeCount} employees</span>
                  </div>
                  {dept.headName && (
                    <div className="dept-head">Head: {dept.headName}</div>
                  )}
                </div>
              </div>
            ))}
      </div>

      {/* Department Tree */}
      {tree && tree.length > 0 && (
        <div className="card" style={{ marginTop: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem', fontSize: '0.95rem', fontWeight: 600 }}>
            Department Hierarchy
          </h3>
          <div className="dept-tree">
            {tree.map(node => (
              <TreeNode key={node.id} dept={node} level={0} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function TreeNode({ dept, level }: { dept: Department; level: number }) {
  const [expanded, setExpanded] = useState(level < 1);
  const hasChildren = dept.children && dept.children.length > 0;

  return (
    <div className="tree-node">
      <div
        className="tree-row"
        style={{ paddingLeft: `${level * 1.5 + 0.5}rem` }}
        onClick={() => hasChildren && setExpanded(!expanded)}
      >
        <span className="tree-toggle">
          {hasChildren ? (
            expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />
          ) : (
            <span style={{ width: 14 }} />
          )}
        </span>
        <Building2 size={14} style={{ color: 'var(--color-primary-hover)', flexShrink: 0 }} />
        <span className="tree-label">{dept.name}</span>
        <span className="tree-count">{dept.employeeCount}</span>
      </div>
      {expanded && hasChildren && (
        <div className="tree-children">
          {dept.children.map(child => (
            <TreeNode key={child.id} dept={child} level={level + 1} />
          ))}
        </div>
      )}
    </div>
  );
}
