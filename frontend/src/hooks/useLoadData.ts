import { useState, useEffect } from 'react';
import { fetchVolumeData, fetchSummaryData, LoadVolumeDataDto } from '../api/dataService';

type DataPoint = {
  date: string;
  volume: number;
};

type UseLoadDataReturn = {
  data: DataPoint[];
  loading: boolean;
  error: string | null;
  summaryData: {
    totalVolume: number;
    averageVolume: number;
  } | null;
};

const useLoadData = (timeframe: 'daily' | 'weekly' | 'monthly'): UseLoadDataReturn => {
  const [data, setData] = useState<DataPoint[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [summaryData, setSummaryData] = useState<{ totalVolume: number; averageVolume: number } | null>(null);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      setError(null);

      try {
        // Fetch volume data based on timeframe
        const volumeData = await fetchVolumeData(timeframe);
        setData(volumeData);

        // Calculate summary data
        if (volumeData && volumeData.length > 0) {
          // Try to fetch summary from API
          try {
            const summary = await fetchSummaryData();
            setSummaryData({
              totalVolume: summary.volume,
              averageVolume: summary.volume / volumeData.length
            });
          } catch (summaryError) {
            // Calculate summary from volume data if API fails
            const total = volumeData.reduce((sum, item) => sum + item.volume, 0);
            setSummaryData({
              totalVolume: total,
              averageVolume: total / volumeData.length
            });
          }
        }
      } catch (err) {
        console.error('Error loading data:', err);
        setError(err instanceof Error ? err.message : 'An unknown error occurred');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [timeframe]);

  return { data, loading, error, summaryData };
};

export default useLoadData;