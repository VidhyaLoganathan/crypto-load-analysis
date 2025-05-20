import React from 'react';
import './LoadingSkeleton.css';

const LoadingSkeleton: React.FC = () => {
  return (
    <div className="loading-skeleton">
      <div className="summary-skeleton">
        <div className="summary-box-skeleton"></div>
        <div className="summary-box-skeleton"></div>
      </div>
      <div className="chart-skeleton">
        <div className="chart-header-skeleton"></div>
        <div className="chart-body-skeleton"></div>
      </div>
    </div>
  );
};

export default LoadingSkeleton;