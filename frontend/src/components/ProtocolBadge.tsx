import React from 'react';
import './ProtocolBadge.css';

type ProtocolBadgeProps = {
  type: 'wallet' | 'contract' | 'protocol' | 'exchange';
  name?: string;
};

const ProtocolBadge: React.FC<ProtocolBadgeProps> = ({ type, name }) => {
  // Determine badge color and icon based on type
  const getBadgeClass = (): string => {
    switch (type) {
      case 'protocol':
        return 'protocol-badge protocol';
      case 'exchange':
        return 'protocol-badge exchange';
      case 'contract':
        return 'protocol-badge contract';
      case 'wallet':
      default:
        return 'protocol-badge wallet';
    }
  };

  const getIcon = (): string => {
    switch (type) {
      case 'protocol':
        return '🔄'; // Protocol icon
      case 'exchange':
        return '💱'; // Exchange icon
      case 'contract':
        return '📜'; // Contract icon
      case 'wallet':
      default:
        return '👛'; // Wallet icon
    }
  };

  return (
    <span className={getBadgeClass()}>
      <span className="icon">{getIcon()}</span>
      <span className="text">{name || type}</span>
    </span>
  );
};

export default ProtocolBadge;