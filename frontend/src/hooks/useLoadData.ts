import { useState, useEffect } from 'react';
import { fetchVolumeData } from '../api/dataService';

type DataPoint = {
  date: string;
  volume: number;
};

type UseLoadDataReturn = {
  data: DataPoint[];
  loading: boolean;
  error: string | null;
};

const useLoadData = (timeframe: 'daily' | 'weekly' | 'monthly'): UseLoadDataReturn => {
  const [data, setData] = useState<DataPoint[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      setError(null);

      try {
        // Get the data from our service
        const volumeData = await fetchVolumeData(timeframe);
        setData(volumeData);
      } catch (err) {
        console.error('Error loading data:', err);
        setError(err instanceof Error ? err.message : 'An unknown error occurred');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [timeframe]);

  return { data, loading, error };
};

export default useLoadData;