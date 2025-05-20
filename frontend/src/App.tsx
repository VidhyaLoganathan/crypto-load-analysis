import React, { useState } from 'react';
import LoadVolumeChart from './components/LoadVolumeChart';
import TimeframeSelector from './components/TimeFrameSelector';
import useLoadData from './hooks/useLoadData';
import LoadingSkeleton from './components/LoadingSkeleton';
import { currentDataSource } from './api/dataService';
import './App.css';

type Timeframe = 'daily' | 'weekly' | 'monthly';

function App() {
  const [timeframe, setTimeframe] = useState<Timeframe>('daily');
  const { data, loading, error, summaryData } = useLoadData(timeframe);

  const handleTimeframeChange = (newTimeframe: Timeframe) => {
    setTimeframe(newTimeframe);
  };

  // Determine the data source badge color and text
  const getDataSourceBadge = (source: string) => {
    let badgeClass = '';
    let text = '';

    switch (source) {
      case 'api':
        badgeClass = 'data-source-badge api';
        text = 'Backend API';
        break;
      case 'simulator':
        badgeClass = 'data-source-badge simulator';
        text = 'Simulator';
        break;
      case 'loading':
        badgeClass = 'data-source-badge loading';
        text = 'Loading...';
        break;
      case 'error':
        badgeClass = 'data-source-badge error';
        text = 'Error (Using Fallback)';
        break;
      default:
        badgeClass = 'data-source-badge unknown';
        text = 'Unknown Source';
    }

    return <span className={badgeClass}>{text}</span>;
  };

  return (
    <div className="App">
      <header className="App-header">
        <h1>Cypher USD Load Volume Visualization (2025)</h1>
      </header>

      <main className="App-main">
        <TimeframeSelector
          selectedTimeframe={timeframe}
          onTimeframeChange={handleTimeframeChange}
        />

        {loading ? (
          <LoadingSkeleton />
        ) : error ? (
          <div className="error-message">
            <p>Error loading data: {error}</p>
            <p>Make sure your backend server is running at the correct URL.</p>
          </div>
        ) : (
          <LoadVolumeChart
            data={data}
            timeframe={timeframe}
            summaryData={summaryData}
          />
        )}

        <div className="info-box">
          <div className="info-row">
            <p>Master Wallet: 0xcCCd218A58B53C67fC17D8C87Cb90d83614e35fD</p>
          </div>

          <div className="info-row data-source-info">
            <div>
              <strong>Volume Data Source:</strong> {getDataSourceBadge(currentDataSource.volumeData)}
            </div>
            <div>
              <strong>Summary Data Source:</strong> {getDataSourceBadge(currentDataSource.summaryData)}
            </div>
          </div>

          <div className="info-row">
            <p>Data Source: Base Chain + Aerodrome Finance (for USD conversion)</p>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;