import React from 'react';
import './TimeFrameSelector.css';

type TimeframeSelectorProps = {
  selectedTimeframe: 'daily' | 'weekly' | 'monthly';
  onTimeframeChange: (timeframe: 'daily' | 'weekly' | 'monthly') => void;
};

const TimeframeSelector: React.FC<TimeframeSelectorProps> = ({
  selectedTimeframe,
  onTimeframeChange
}) => {
  return (
    <div className="timeframe-selector">
      <span>View by: </span>
      <div className="button-group">
        <button
          className={`timeframe-button ${selectedTimeframe === 'daily' ? 'active' : ''}`}
          onClick={() => onTimeframeChange('daily')}
        >
          Daily
        </button>
        <button
          className={`timeframe-button ${selectedTimeframe === 'weekly' ? 'active' : ''}`}
          onClick={() => onTimeframeChange('weekly')}
        >
          Weekly
        </button>
        <button
          className={`timeframe-button ${selectedTimeframe === 'monthly' ? 'active' : ''}`}
          onClick={() => onTimeframeChange('monthly')}
        >
          Monthly
        </button>
      </div>
    </div>
  );
};

export default TimeframeSelector;