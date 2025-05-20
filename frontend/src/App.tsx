import React, { useState } from 'react';
import LoadVolumeChart from './components/LoadVolumeChart';
import TimeframeSelector from './components/TimeFrameSelector';
import useLoadData from './hooks/useLoadData';
import LoadingSkeleton from './components/LoadingSkeleton';
import './App.css';

type Timeframe = 'daily' | 'weekly' | 'monthly';

function App() {
  const [timeframe, setTimeframe] = useState<Timeframe>('daily');
  const { data, loading, error } = useLoadData(timeframe);

  const handleTimeframeChange = (newTimeframe: Timeframe) => {
    setTimeframe(newTimeframe);
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
          <div className="error-message">Error loading data: {error}</div>
        ) : (
          <LoadVolumeChart data={data} timeframe={timeframe} />
        )}

        <div className="info-box">
          <p>Master Wallet: 0xcCCd218A58B53C67fC17D8C87Cb90d83614e35fD</p>
          <p>Data Source: Base Chain + Aerodrome Finance (for USD conversion)</p>
        </div>
      </main>
    </div>
  );
}

export default App;