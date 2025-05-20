import axios from 'axios';
import { format, parseISO, startOfYear, subDays } from 'date-fns';

// API base URL - adjust based on how your Spring Boot app is configured
// If you're using proxy in package.json, leave this empty
const API_BASE_URL = '';

// Spring Boot controller context path
// This should match your controller's @RequestMapping path
const API_CONTEXT_PATH = '/api/load-volume';

// Flag to force using mock data during development/debugging
// Set to true to bypass API calls and use mock data instead
const USE_MOCK_DATA = false;

// Data source tracking
export let currentDataSource = {
  volumeData: 'loading', // 'api', 'simulator', 'loading', or 'error'
  summaryData: 'loading'  // 'api', 'simulator', 'loading', or 'error'
};

// Custom type for backend data format (matching the actual backend response)
export type LoadVolumeDataDto = {
  date: string;             // ISO date string
  totalUsdValue: number;    // Total USD value (backend uses this name instead of 'volume')
  tokenBreakdown?: {
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
  // Update data source state
  currentDataSource.volumeData = 'loading';

  // If mock data is enabled, skip API call
  if (USE_MOCK_DATA) {
    console.log('Using mock data (forced by flag)');
    currentDataSource.volumeData = 'simulator';
    return generateMockData(timeframe);
  }

  try {
    const dateRange = getDateRange();

    // Construct the API URL based on timeframe
    const apiUrl = `${API_BASE_URL}${API_CONTEXT_PATH}/${timeframe}?startDate=${dateRange.start}&endDate=${dateRange.end}`;

    console.log(`Fetching data from: ${apiUrl}`);

    // Make the API request
    const response = await axios.get<LoadVolumeDataDto[]>(apiUrl);

    console.log('API Response:', response.data);

    // Check if response data is valid
    if (!response.data || !Array.isArray(response.data)) {
      console.warn('Invalid response format from API:', response.data);
      currentDataSource.volumeData = 'simulator';
      return generateMockData(timeframe); // Fall back to mock data
    }

    // Set data source to API since request was successful
    currentDataSource.volumeData = 'api';

    // Transform the data to match our frontend format - mapping totalUsdValue to volume
    return response.data.map(item => ({
      date: formatDateForDisplay(item.date || '', timeframe),
      volume: typeof item.totalUsdValue === 'number' ? item.totalUsdValue : 0,
      tokenBreakdown: item.tokenBreakdown || {}
    }));
  } catch (error) {
    console.error(`Error fetching ${timeframe} volume data:`, error);
    console.log('Falling back to mock data');

    // Set data source to simulator due to error
    currentDataSource.volumeData = 'error';

    // If API call fails, fall back to mock data for development
    return generateMockData(timeframe);
  }
}

// Format dates for display in the UI
function formatDateForDisplay(dateStr: string, timeframe: 'daily' | 'weekly' | 'monthly'): string {
  try {
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
  } catch (e) {
    console.error('Error parsing date:', dateStr, e);
    return dateStr || 'Unknown Date';
  }
}

// Fetch summary data from backend API
export async function fetchSummaryData() {
  // Update data source state
  currentDataSource.summaryData = 'loading';

  // If mock data is enabled, skip API call
  if (USE_MOCK_DATA) {
    console.log('Using mock summary data (forced by flag)');
    currentDataSource.summaryData = 'simulator';
    return generateMockSummary();
  }

  try {
    const dateRange = getDateRange();

    const apiUrl = `${API_BASE_URL}${API_CONTEXT_PATH}/summary?startDate=${dateRange.start}&endDate=${dateRange.end}`;

    console.log(`Fetching summary from: ${apiUrl}`);

    const response = await axios.get(apiUrl);

    console.log('Summary API Response:', response.data);

    // More lenient validation - just ensure there's some response
    if (!response.data) {
      console.warn('Empty summary response');
      currentDataSource.summaryData = 'simulator';
      return generateMockSummary();
    }

    // Try to extract volume, with fallback
    const volume = extractVolumeFromResponse(response.data);

    // Set data source to API since request was successful
    currentDataSource.summaryData = 'api';

    return {
      date: 'Total',
      volume: volume
    };
  } catch (error) {
    console.error('Error fetching summary data:', error);
    currentDataSource.summaryData = 'error';
    return generateMockSummary();
  }
}

// Helper function to safely extract volume from various response formats
function extractVolumeFromResponse(responseData: any): number {
  try {
    // Log the type of response for debugging
    console.log('Summary response type:', typeof responseData);

    // Case 1: Direct number
    if (typeof responseData === 'number') {
      return responseData;
    }

    // Case 2: Object with volume property
    if (typeof responseData === 'object' && responseData !== null) {
      console.log('Response keys:', Object.keys(responseData));

      // Case 2.1: Direct totalUsdValue property (matches your backend)
      if (typeof responseData.totalUsdValue === 'number') {
        return responseData.totalUsdValue;
      }

      // Case 2.2: Direct volume property (fallback)
      if (typeof responseData.volume === 'number') {
        return responseData.volume;
      }

      // Case 2.3: Try to find 'totalUsdValue' or 'volume' as string that can be parsed
      if (typeof responseData.totalUsdValue === 'string') {
        const parsed = parseFloat(responseData.totalUsdValue);
        if (!isNaN(parsed)) {
          return parsed;
        }
      }

      if (typeof responseData.volume === 'string') {
        const parsed = parseFloat(responseData.volume);
        if (!isNaN(parsed)) {
          return parsed;
        }
      }

      // Case 2.4: If this is an array response for the summary endpoint, sum up totalUsdValue
      if (Array.isArray(responseData)) {
        console.log('Received array response for summary endpoint, calculating total');
        return responseData.reduce((sum, item) => {
          const value = typeof item.totalUsdValue === 'number' ? item.totalUsdValue : 0;
          return sum + value;
        }, 0);
      }

      // Case 2.5: Try to find a numeric property
      for (const key of Object.keys(responseData)) {
        if (typeof responseData[key] === 'number') {
          console.log(`Found numeric property '${key}' with value ${responseData[key]}`);
          return responseData[key];
        }
      }
    }

    // If we get here, we couldn't find a valid volume
    console.warn('Could not extract volume from response:', responseData);
    currentDataSource.summaryData = 'error';
    return 0;
  } catch (err) {
    console.error('Error extracting volume:', err);
    currentDataSource.summaryData = 'error';
    return 0;
  }
}

// Generate a mock summary
function generateMockSummary() {
  return {
    date: 'Total',
    volume: 1500000 // Mock $1.5M volume
  };
}

// ------------------------
// Mock data fallback functionality (for development/testing)
// ------------------------

// Generate mock data (fallback if API is unavailable)
function generateMockData(timeframe: 'daily' | 'weekly' | 'monthly') {
  console.log(`Generating mock data for ${timeframe} timeframe`);

  const mockData = [];
  const startDate = new Date(2025, 0, 1); // Jan 1, 2025
  const endDate = new Date(2025, 4, 19);  // May 19, 2025

  let currentDate = new Date(startDate);
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
      volume: Math.round(10000 + Math.random() * 90000), // Random volume between $10k-$100k
      tokenBreakdown: {
        ETH: Math.round(Math.random() * 50000),
        USDC: Math.round(Math.random() * 30000),
        DAI: Math.round(Math.random() * 20000)
      }
    });

    // Increment date based on timeframe
    const nextDate = new Date(currentDate);
    nextDate.setDate(nextDate.getDate() + increment);
    currentDate = nextDate;
  }

  console.log(`Generated ${mockData.length} mock data points`);
  return mockData;
}