import axios from 'axios';
import { format, parseISO, startOfYear, subDays } from 'date-fns';

// API base URL - adjust as needed based on your deployment setup
const API_BASE_URL = 'http://localhost:8080'; // Change to your Spring Boot API URL

// Custom type for backend data format
export type LoadVolumeDataDto = {
  date: string;      // ISO date string
  volume: number;    // USD volume
  tokenBreakdown?: { // Optional token breakdown
    [token: string]: number;
  };
};

// Format dates for API requests (ISO format)
const formatDateForApi = (date: Date): string => {
  return format(date, 'yyyy-MM-dd');
};

// Get the date range for 2025 data
const getDateRange = () => {
  const startDate = new Date(2025, 0, 1); // Jan 1, 2025
  const today = new Date(2025, 4, 19);    // May 19, 2025 (current date in your simulation)

  return {
    start: formatDateForApi(startDate),
    end: formatDateForApi(today)
  };
};

// Fetch load volume data from backend API
export async function fetchVolumeData(timeframe: 'daily' | 'weekly' | 'monthly') {
  try {
    const dateRange = getDateRange();

    // Construct the API URL based on timeframe
    const apiUrl = `${API_BASE_URL}/api/load-volume/${timeframe}?startDate=${dateRange.start}&endDate=${dateRange.end}`;

    // Make the API request
    const response = await axios.get<LoadVolumeDataDto[]>(apiUrl);

    // Transform the data to match our frontend format
    return response.data.map(item => ({
      date: formatDateForDisplay(item.date, timeframe),
      volume: item.volume
    }));
  } catch (error) {
    console.error(`Error fetching ${timeframe} volume data:`, error);

    // If API call fails, fall back to mock data for development
    console.warn('Falling back to mock data');
    return generateMockData(timeframe);
  }
}

// Format dates for display in the UI
function formatDateForDisplay(dateStr: string, timeframe: 'daily' | 'weekly' | 'monthly'): string {
  const date = parseISO(dateStr);

  switch (timeframe) {
    case 'daily':
      return format(date, 'MMM d');
    case 'weekly':
      return format(date, "'Week of' MMM d");
    case 'monthly':
      return format(date, 'MMMM yyyy');
    default:
      return dateStr;
  }
}

// Fetch summary data from backend API
export async function fetchSummaryData() {
  try {
    const dateRange = getDateRange();

    const apiUrl = `${API_BASE_URL}/api/load-volume/summary?startDate=${dateRange.start}&endDate=${dateRange.end}`;

    const response = await axios.get<LoadVolumeDataDto>(apiUrl);
    return response.data;
  } catch (error) {
    console.error('Error fetching summary data:', error);

    // Return a basic mock summary if the API call fails
    return {
      date: 'Total',
      volume: 1000000 // Mock $1M volume
    };
  }
}

// ------------------------
// Mock data fallback functionality (for development/testing)
// ------------------------

// Generate mock data (fallback if API is unavailable)
function generateMockData(timeframe: 'daily' | 'weekly' | 'monthly') {
  const mockData = [];
  const startDate = new Date(2025, 0, 1); // Jan 1, 2025
  const endDate = new Date(2025, 4, 19);  // May 19, 2025

  let currentDate = startDate;
  let increment = 1;
  let formatStr = 'MMM d';

  // Set increment and format based on timeframe
  if (timeframe === 'weekly') {
    increment = 7;
    formatStr = "'Week of' MMM d";
  } else if (timeframe === 'monthly') {
    increment = 30;
    formatStr = 'MMMM yyyy';
  }

  // Generate data points
  while (currentDate <= endDate) {
    mockData.push({
      date: format(currentDate, formatStr),
      volume: Math.round(10000 + Math.random() * 90000) // Random volume between $10k-$100k
    });

    // Increment date based on timeframe
    currentDate = new Date(currentDate);
    currentDate.setDate(currentDate.getDate() + increment);
  }

  return mockData;
}