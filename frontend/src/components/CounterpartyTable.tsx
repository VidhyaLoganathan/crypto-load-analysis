import React from 'react';
import { Counterparty, formatAddress } from '../api/walletService';
import ProtocolBadge from './ProtocolBadge';
import './CounterpartyTable.css';

type CounterpartyTableProps = {
  counterparties: Counterparty[];
  walletAddress: string;
};

const CounterpartyTable: React.FC<CounterpartyTableProps> = ({
  counterparties,
  walletAddress
}) => {
  // If no counterparties, show empty state
  if (!counterparties || counterparties.length === 0) {
    return (
      <div className="empty-state">
        <p>No counterparties found for this wallet address.</p>
      </div>
    );
  }

  return (
    <div className="counterparty-table-container">
      <h2>Top Counterparties for {formatAddress(walletAddress)}</h2>

      <table className="counterparty-table">
        <thead>
          <tr>
            <th>Rank</th>
            <th>Address</th>
            <th>Name</th>
            <th>Type</th>
            <th>Tx Count</th>
          </tr>
        </thead>
        <tbody>
          {counterparties.map((counterparty, index) => (
            <tr key={counterparty.address} className={index % 2 === 0 ? 'even-row' : 'odd-row'}>
              <td className="rank-cell">{index + 1}</td>
              <td className="address-cell">
                <a
                  href={`https://basescan.org/address/${counterparty.address}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="address-link"
                >
                  {formatAddress(counterparty.address)}
                </a>
              </td>
              <td className="name-cell">{counterparty.name}</td>
              <td className="type-cell">
                <ProtocolBadge
                  type={counterparty.type}
                  name={counterparty.protocol || counterparty.type}
                />
              </td>
              <td className="count-cell">{counterparty.transactionCount}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="table-footer">
        <p>Click on an address to view details on BaseScan</p>
      </div>
    </div>
  );
};

export default CounterpartyTable;