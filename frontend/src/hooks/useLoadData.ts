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

        // Ensure volume data is properly formatted
        const validData = volumeData.map(item => ({
          date: item.date || '',
          volume: typeof item.volume === 'number' && !isNaN(item.volume) ? item.volume : 0
        }));

        setData(validData);

        // Calculate summary data
        if (validData && validData.length > 0) {
          try {
            // Try to fetch summary from API
            const summary = await fetchSummaryData();

            // Validate summary data
            const totalVolume = typeof summary.volume === 'number' && !isNaN(summary.volume)
              ? summary.volume
              : 0;

            setSummaryData({
              totalVolume,
              averageVolume: validData.length > 0 ? totalVolume / validData.length : 0
            });
          } catch (summaryError) {
            // Calculate summary from volume data if API fails
            const total = validData.reduce((sum, item) => sum + (item.volume || 0), 0);
            setSummaryData({
              totalVolume: total,
              averageVolume: validData.length > 0 ? total / validData.length : 0
            });
          }
        } else {
          // Set default summary data if no valid data
          setSummaryData({
            totalVolume: 0,
            averageVolume: 0
          });
        }
      } catch (err) {
        console.error('Error loading data:', err);
        setError(err instanceof Error ? err.message : 'An unknown error occurred');

        // Set empty data and default summary values on error
        setData([]);
        setSummaryData({
          totalVolume: 0,
          averageVolume: 0
        });
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [timeframe]);

  return { data, loading, error, summaryData };
};

export default useLoadData;