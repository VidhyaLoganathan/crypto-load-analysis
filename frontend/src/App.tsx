import React, { useState } from 'react';
import LoadVolumePage from './pages/LoadVolumePage';
import WalletAnalysisPage from './pages/WalletAnalysisPage';
import './App.css';

const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'load-volume' | 'wallet-analysis'>('load-volume');

  return (
    <div className="App">
      <nav className="app-nav">
        <div className="tab-container">
          <button
            className={`tab ${activeTab === 'load-volume' ? 'active' : ''}`}
            onClick={() => setActiveTab('load-volume')}
          >
            USD Load Volume
          </button>
          <button
            className={`tab ${activeTab === 'wallet-analysis' ? 'active' : ''}`}
            onClick={() => setActiveTab('wallet-analysis')}
          >
            Wallet Analysis
          </button>
        </div>
      </nav>

      <div className="app-content">
        {activeTab === 'load-volume' ? (
          <LoadVolumePage />
        ) : (
          <WalletAnalysisPage />
        )}
      </div>

      <footer className="app-footer">
        <p>Cypher Blockchain Analytics - Take-Home Test</p>
      </footer>
    </div>
  );
};

export default App;