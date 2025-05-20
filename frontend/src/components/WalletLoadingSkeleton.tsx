import React from 'react';
import './WalletLoadingSkeleton.css';

const LoadingSkeleton: React.FC = () => {
  return (
    <div className="loading-skeleton wallet-loading">
      <div className="table-skeleton-header"></div>

      <div className="table-skeleton">
        <div className="table-skeleton-row header-row"></div>
        {Array(10).fill(0).map((_, index) => (
          <div key={index} className="table-skeleton-row"></div>
        ))}
      </div>
    </div>
  );
};

export default LoadingSkeleton;