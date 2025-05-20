import { useState, useEffect } from 'react';
import { fetchWalletAnalysis, WalletAnalysisData, Counterparty } from '../api/walletService';

type UseWalletDataReturn = {
  data: WalletAnalysisData | null;
  loading: boolean;
  error: string | null;
};

const useWalletData = (walletAddress: string): UseWalletDataReturn => {
  const [data, setData] = useState<WalletAnalysisData | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Don't make a request if address is empty
    if (!walletAddress) {
      setData(null);
      setError(null);
      setLoading(false);
      return;
    }

    const loadData = async () => {
      setLoading(true);
      setError(null);

      try {
        // Basic validation
        if (!walletAddress.startsWith('0x') || walletAddress.length !== 42) {
          throw new Error('Invalid wallet address format. Address should start with 0x and be 42 characters long.');
        }

        // Fetch wallet analysis data
        const walletData = await fetchWalletAnalysis(walletAddress);
        setData(walletData);
      } catch (err) {
        console.error('Error loading wallet data:', err);
        setError(err instanceof Error ? err.message : 'An unknown error occurred');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [walletAddress]);

  return { data, loading, error };
};

export default useWalletData;