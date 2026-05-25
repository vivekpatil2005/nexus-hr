import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../lib/api';
import type { OrgChartNode } from '../lib/types';
import { ZoomIn, ZoomOut, Maximize2 } from 'lucide-react';
import './OrgChartPage.css';

export default function OrgChartPage() {
  const [zoom, setZoom] = useState(1);

  const { data: orgChart, isLoading } = useQuery({
    queryKey: ['org-chart'],
    queryFn: () => api.get<{ data: OrgChartNode[] }>('/org-chart').then(r => r.data.data),
  });

  return (
    <div className="page-content">
      <div className="page-header">
        <div>
          <h1>Organization Chart</h1>
          <p>Visual hierarchy of your organization</p>
        </div>
        <div className="zoom-controls">
          <button className="btn btn-ghost" onClick={() => setZoom(z => Math.max(0.3, z - 0.1))}>
            <ZoomOut size={16} />
          </button>
          <span className="zoom-level">{Math.round(zoom * 100)}%</span>
          <button className="btn btn-ghost" onClick={() => setZoom(z => Math.min(2, z + 0.1))}>
            <ZoomIn size={16} />
          </button>
          <button className="btn btn-ghost" onClick={() => setZoom(1)}>
            <Maximize2 size={16} />
          </button>
        </div>
      </div>

      <div className="org-chart-container card">
        {isLoading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: '3rem' }}>
            <div className="spinner" />
          </div>
        ) : orgChart && orgChart.length > 0 ? (
          <div className="org-chart-scroll">
            <div className="org-chart-tree" style={{ transform: `scale(${zoom})`, transformOrigin: 'top center' }}>
              {orgChart.map(node => (
                <OrgNode key={node.id} node={node} />
              ))}
            </div>
          </div>
        ) : (
          <div className="empty-state">
            <p>No org chart data. Add employees with manager relationships to build the hierarchy.</p>
          </div>
        )}
      </div>
    </div>
  );
}

function OrgNode({ node }: { node: OrgChartNode }) {
  const initials = node.name
    .split(' ')
    .map(n => n[0])
    .join('')
    .substring(0, 2);

  return (
    <div className="org-node-wrapper">
      <div className="org-node">
        <div className="avatar">{initials}</div>
        <div className="org-node-info">
          <div className="org-node-name">{node.name}</div>
          <div className="org-node-title">{node.designation || 'Employee'}</div>
          {node.departmentName && (
            <div className="org-node-dept">{node.departmentName}</div>
          )}
        </div>
      </div>
      {node.children && node.children.length > 0 && (
        <div className="org-children">
          {node.children.map(child => (
            <OrgNode key={child.id} node={child} />
          ))}
        </div>
      )}
    </div>
  );
}
