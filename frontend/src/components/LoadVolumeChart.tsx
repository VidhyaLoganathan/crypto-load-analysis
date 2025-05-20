import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import './LoadVolumeChart.css';

type DataPoint = {
  date: string;
  volume: number;
};

type TimeframeData = DataPoint[];

type LoadVolumeChartProps = {
  data: TimeframeData;
  timeframe: 'daily' | 'weekly' | 'monthly';
  summaryData: {
    totalVolume: number;
    averageVolume: number;
  } | null;
};

const LoadVolumeChart: React.FC<LoadVolumeChartProps> = ({ data, timeframe, summaryData }) => {
  // Format the tooltip to display USD values
  const formatTooltip = (value: number) => {
    if (isNaN(value)) return "$0.00";
    return `$${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  // Format number for display, handling NaN values
  const formatCurrency = (value: number) => {
    if (isNaN(value)) return "$0.00";
    return `$${value.toLocaleString(undefined, { maximumFractionDigits: 2 })}`;
  };

  // Determine the X-axis label based on the timeframe
  const getXAxisLabel = () => {
    switch (timeframe) {
      case 'daily':
        return 'Date';
      case 'weekly':
        return 'Week';
      case 'monthly':
        return 'Month';
      default:
        return 'Date';
    }
  };

  // Ensure data array is valid
  const validData = Array.isArray(data) ? data : [];

  // Calculate values from data
  const calculateTotalVolume = (): number => {
    if (!validData.length) return 0;

    return validData.reduce((sum, point) => {
      const volume = typeof point.volume === 'number' ? point.volume : 0;
      return sum + volume;
    }, 0);
  };

  const calculateAverageVolume = (total: number): number => {
    if (!validData.length) return 0;
    return total / validData.length;
  };

  // Calculate values with fallbacks
  let totalVolume = 0;
  let averageVolume = 0;

  try {
    // Try to use summary data if available
    if (summaryData && typeof summaryData.totalVolume === 'number' && !isNaN(summaryData.totalVolume)) {
      totalVolume = summaryData.totalVolume;
      averageVolume = typeof summaryData.averageVolume === 'number' && !isNaN(summaryData.averageVolume)
        ? summaryData.averageVolume
        : calculateAverageVolume(totalVolume);
    } else {
      // Otherwise calculate from data
      totalVolume = calculateTotalVolume();
      averageVolume = calculateAverageVolume(totalVolume);
    }
  } catch (error) {
    console.error("Error calculating volume metrics:", error);
    totalVolume = 0;
    averageVolume = 0;
  }

  return (
    <div className="chart-container">
      <div className="chart-summary">
        <div className="summary-box">
          <h3>Total Volume</h3>
          <p>{formatCurrency(totalVolume)}</p>
        </div>
        <div className="summary-box">
          <h3>Average {timeframe.charAt(0).toUpperCase() + timeframe.slice(1)} Volume</h3>
          <p>{formatCurrency(averageVolume)}</p>
        </div>
      </div>

      <div className="chart-wrapper">
        <ResponsiveContainer width="100%" height={400}>
          <LineChart
            data={validData}
            margin={{
              top: 5,
              right: 30,
              left: 20,
              bottom: 5,
            }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" label={{ value: getXAxisLabel(), position: 'insideBottomRight', offset: -10 }} />
            <YAxis
              label={{ value: 'USD Volume', angle: -90, position: 'insideLeft' }}
              tickFormatter={(value) => isNaN(value) ? "$0" : `$${value.toLocaleString()}`}
            />
            <Tooltip formatter={formatTooltip} />
            <Legend />
            <Line
              type="monotone"
              dataKey="volume"
              name="USD Load Volume"
              stroke="#8884d8"
              activeDot={{ r: 8 }}
              strokeWidth={2}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Token Breakdown (Optional - implement if needed) */}
      {/*
      <div className="token-breakdown">
        <h3>Token Breakdown</h3>
        <div className="token-list">
          {Object.entries(data[0]?.tokenBreakdown || {}).map(([token, amount]) => (
            <div className="token-item" key={token}>
              <span className="token-name">{token}</span>
              <span className="token-amount">{formatCurrency(amount)}</span>
            </div>
          ))}
        </div>
      </div>
      */}
    </div>
  );
};

export default LoadVolumeChart;