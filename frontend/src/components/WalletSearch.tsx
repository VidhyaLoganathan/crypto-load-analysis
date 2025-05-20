import React, { useState } from 'react';
import './WalletSearch.css';

type WalletSearchProps = {
  onSearch: (address: string) => void;
};

const WalletSearch: React.FC<WalletSearchProps> = ({ onSearch }) => {
  const [walletAddress, setWalletAddress] = useState<string>('');
  const [isValid, setIsValid] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string>('');

  // Validate wallet address (basic Ethereum address format)
  const validateAddress = (address: string): boolean => {
    if (!address) {
      setErrorMessage('Wallet address is required');
      return false;
    }

    if (!address.startsWith('0x')) {
      setErrorMessage('Wallet address must start with 0x');
      return false;
    }

    if (address.length !== 42) {
      setErrorMessage('Wallet address must be 42 characters long');
      return false;
    }

    // Check if address contains only hex characters (0-9, a-f, A-F)
    const hexRegex = /^0x[0-9a-fA-F]{40}$/;
    if (!hexRegex.test(address)) {
      setErrorMessage('Wallet address must only contain hex characters');
      return false;
    }

    return true;
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setWalletAddress(value);

    // Clear error state when user types
    if (!isValid) {
      setIsValid(true);
      setErrorMessage('');
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const isAddressValid = validateAddress(walletAddress);
    setIsValid(isAddressValid);

    if (isAddressValid) {
      onSearch(walletAddress);
    }
  };

  const handlePaste = () => {
    // Clean up the address when pasted (remove whitespace)
    setTimeout(() => {
      setWalletAddress(prev => prev.trim());
    }, 0);
  };

  // Sample addresses for testing
  const sampleAddresses = [
    '0x68b3465833fb72a70ecdf485e0e4c7bd8665fc45', // Uniswap Router
    '0xdef1c0ded9bec7f1a1670819833240f027b25eff', // Coinbase
    '0xcCCd218A58B53C67fC17D8C87Cb90d83614e35fD'  // Cypher Master Wallet
  ];

  const handleSampleClick = (address: string) => {
    setWalletAddress(address);
    setIsValid(true);
    setErrorMessage('');
    onSearch(address);
  };

  return (
    <div className="wallet-search-container">
      <form onSubmit={handleSubmit} className="wallet-search-form">
        <div className="input-group">
          <input
            type="text"
            value={walletAddress}
            onChange={handleInputChange}
            onPaste={handlePaste}
            placeholder="Enter wallet address (0x...)"
            className={`wallet-input ${!isValid ? 'wallet-input-error' : ''}`}
          />
          <button type="submit" className="search-button">
            Analyze
          </button>
        </div>

        {!isValid && (
          <div className="error-message">{errorMessage}</div>
        )}
      </form>

      <div className="sample-addresses">
        <span>Try sample addresses: </span>
        {sampleAddresses.map((address, index) => (
          <button
            key={index}
            onClick={() => handleSampleClick(address)}
            className="sample-address-button"
          >
            {address.substring(0, 6)}...{address.substring(address.length - 4)}
          </button>
        ))}
      </div>
    </div>
  );
};

export default WalletSearch;