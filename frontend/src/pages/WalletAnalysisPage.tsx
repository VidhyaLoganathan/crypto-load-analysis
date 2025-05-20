import React, { useState } from 'react';
import WalletSearch from '../components/WalletSearch';
import CounterpartyTable from '../components/CounterpartyTable';
import WalletLoadingSkeleton from '../components/WalletLoadingSkeleton';
import useWalletData from '../hooks/useWalletData';
import { walletDataSource } from '../api/walletService';
import './WalletAnalysisPage.css';

const WalletAnalysisPage: React.FC = () => {
  const [walletAddress, setWalletAddress] = useState<string>('');
  const { data, loading, error } = useWalletData(walletAddress);

  const handleSearch = (address: string) => {
    setWalletAddress(address);
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
    <div className="wallet-analysis-page">
      <header className="wallet-header">
        <h1>Wallet Counterparty Analysis</h1>
      </header>

      <main className="wallet-main">
        <div className="search-container">
          <WalletSearch onSearch={handleSearch} />
        </div>

        <div className="results-container">
          {!walletAddress ? (
            <div className="empty-state">
              <p>Enter a wallet address to see its top counterparties</p>
            </div>
          ) : loading ? (
            <WalletLoadingSkeleton />
          ) : error ? (
            <div className="error-message">
              <p>Error: {error}</p>
            </div>
          ) : data ? (
            <CounterpartyTable
              counterparties={data.topCounterparties}
              walletAddress={data.walletAddress}
            />
          ) : null}
        </div>

        <div className="info-box">
          <div className="info-row">
            <p>This analysis shows the top 10 counterparties a wallet has interacted with on Base.</p>
          </div>

          <div className="info-row data-source-info">
            <div>
              <strong>Data Source:</strong> {getDataSourceBadge(walletDataSource)}
            </div>
          </div>

          <div className="info-row">
            <p>Counterparties are identified as wallets, contracts, protocols, or exchanges when possible.</p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default WalletAnalysisPage;