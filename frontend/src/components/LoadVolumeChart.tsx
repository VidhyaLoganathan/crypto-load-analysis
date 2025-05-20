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
};

const LoadVolumeChart: React.FC<LoadVolumeChartProps> = ({ data, timeframe }) => {
  // Format the tooltip to display USD values
  const formatTooltip = (value: number) => {
    return `$${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
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

  // Calculate some summary statistics
  const totalVolume = data.reduce((sum, point) => sum + point.volume, 0);
  const averageVolume = totalVolume / data.length;

  return (
    <div className="chart-container">
      <div className="chart-summary">
        <div className="summary-box">
          <h3>Total Volume</h3>
          <p>${totalVolume.toLocaleString()}</p>
        </div>
        <div className="summary-box">
          <h3>Average {timeframe.charAt(0).toUpperCase() + timeframe.slice(1)} Volume</h3>
          <p>${averageVolume.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
        </div>
      </div>

      <div className="chart-wrapper">
        <ResponsiveContainer width="100%" height={400}>
          <LineChart
            data={data}
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
              tickFormatter={(value) => `$${value.toLocaleString()}`}
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
    </div>
  );
};

export default LoadVolumeChart;