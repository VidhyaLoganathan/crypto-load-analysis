import { ethers } from 'ethers';
import { format, startOfMonth, endOfMonth, startOfWeek, endOfWeek, eachDayOfInterval, eachWeekOfInterval, eachMonthOfInterval, startOfYear, endOfYear } from 'date-fns';

// Constants
const MASTER_WALLET = '0xcCCd218A58B53C67fC17D8C87Cb90d83614e35fD';
const BASE_RPC_URL = 'https://mainnet.base.org';

// Token mappings for known ERC-20s
const TOKEN_ADDRESSES: Record<string, string> = {
  '0x4200000000000000000000000000000000000006': 'ETH', // Wrapped ETH on Base
  '0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913': 'USDC',
  // Add other tokens as needed
};

// Sample exchange rate function - in a real app, this would interface with Aerodrome
async function getHistoricalPrice(tokenAddress: string, timestamp: number): Promise<number> {
  // In a real implementation, this would call Aerodrome Finance contracts
  // For demo purposes, we'll return mock data

  // Sample rates (would be replaced with actual API calls)
  const MOCK_PRICES: Record<string, string> = {
    'ETH': '3500', // $3500 per ETH
    'USDC': '1.0',  // $1 per USDC
  };

  const tokenSymbol = TOKEN_ADDRESSES[tokenAddress] || 'UNKNOWN';
  return parseFloat(MOCK_PRICES[tokenSymbol] || '1.0');
}

// Function to fetch transactions for the master wallet
async function fetchTransactions() {
  try {
    // Ethers v5 syntax for creating a provider
    const provider = new ethers.providers.JsonRpcProvider(BASE_RPC_URL);

    // In a real implementation, we would:
    // 1. Query token transfer events (ERC-20 Transfer events)
    // 2. Get native ETH transfers to the master wallet

    // For demo purposes, returning mock transaction data for 2025
    return generateMockTransactions();
  } catch (error) {
    console.error('Error fetching transactions:', error);
    throw error;
  }
}

// Generate mock transaction data for demonstration
function generateMockTransactions() {
  // Create date range for 2025
  const year2025 = {
    start: new Date(2025, 0, 1), // Jan 1, 2025
    end: new Date(2025, 4, 19),  // May 19, 2025 (current date)
  };

  // Generate transactions with random amounts for each day
  const transactions = [];
  const days = eachDayOfInterval(year2025);

  for (const day of days) {
    // Random number of transactions per day (0-5)
    const txCount = Math.floor(Math.random() * 6);

    for (let i = 0; i < txCount; i++) {
      // Random token selection
      const tokenAddresses = Object.keys(TOKEN_ADDRESSES);
      const tokenAddress = tokenAddresses[Math.floor(Math.random() * tokenAddresses.length)];
      const tokenSymbol = TOKEN_ADDRESSES[tokenAddress];

      // Random amount based on token
      let amount;
      if (tokenSymbol === 'ETH') {
        // ETH amount between 0.1 and 2 ETH
        amount = 0.1 + Math.random() * 1.9;
      } else {
        // USDC amount between 100 and 5000
        amount = 100 + Math.random() * 4900;
      }

      transactions.push({
        timestamp: day.getTime() / 1000, // Unix timestamp
        tokenAddress,
        tokenSymbol,
        amount,
        hash: '0x' + Math.random().toString(16).substr(2, 64), // Random hash
      });
    }
  }

  return transactions;
}

// Calculate USD values for each transaction
async function calculateUSDValues(transactions: any[]) {
  const txWithUSD = [];

  for (const tx of transactions) {
    const price = await getHistoricalPrice(tx.tokenAddress, tx.timestamp);
    const usdValue = tx.amount * price;

    txWithUSD.push({
      ...tx,
      usdValue,
    });
  }

  return txWithUSD;
}

// Aggregate data by timeframe (daily, weekly, monthly)
function aggregateByTimeframe(transactions: any[], timeframe: 'daily' | 'weekly' | 'monthly') {
  // Get all transactions from 2025
  const year2025 = {
    start: new Date(2025, 0, 1),
    end: new Date(2025, 4, 19), // Current date (May 19, 2025)
  };

  let intervals: Date[];
  let formatString: string;

  // Set up the intervals and format string based on timeframe
  switch (timeframe) {
    case 'daily':
      intervals = eachDayOfInterval(year2025);
      formatString = 'MMM d';
      break;
    case 'weekly':
      intervals = eachWeekOfInterval(year2025, { weekStartsOn: 1 }); // Week starts Monday
      formatString = "'Week of' MMM d";
      break;
    case 'monthly':
      intervals = eachMonthOfInterval(year2025);
      formatString = 'MMMM yyyy';
      break;
    default:
      intervals = eachDayOfInterval(year2025);
      formatString = 'MMM d';
  }

  // Initialize result array with zero volumes
  const result = intervals.map(interval => {
    let start, end;

    if (timeframe === 'daily') {
      start = interval;
      end = interval;
    } else if (timeframe === 'weekly') {
      start = startOfWeek(interval, { weekStartsOn: 1 });
      end = endOfWeek(interval, { weekStartsOn: 1 });
    } else { // monthly
      start = startOfMonth(interval);
      end = endOfMonth(interval);
    }

    return {
      date: format(interval, formatString),
      startTimestamp: start.getTime() / 1000,
      endTimestamp: end.getTime() / 1000,
      volume: 0,
    };
  });

  // Aggregate volumes within each interval
  for (const tx of transactions) {
    const txDate = new Date(tx.timestamp * 1000);
    const periodIndex = result.findIndex(period =>
      tx.timestamp >= period.startTimestamp && tx.timestamp <= period.endTimestamp
    );

    if (periodIndex !== -1) {
      result[periodIndex].volume += tx.usdValue;
    }
  }

  // Round volumes to 2 decimal places
  return result.map(period => ({
    date: period.date,
    volume: Math.round(period.volume * 100) / 100,
  }));
}

// Main function to fetch volume data
export async function fetchVolumeData(timeframe: 'daily' | 'weekly' | 'monthly') {
  try {
    // 1. Fetch all transactions to the master wallet
    const transactions = await fetchTransactions();

    // 2. Calculate USD values for each transaction
    const txWithUSD = await calculateUSDValues(transactions);

    // 3. Aggregate by the requested timeframe
    const aggregatedData = aggregateByTimeframe(txWithUSD, timeframe);

    return aggregatedData;
  } catch (error) {
    console.error('Error in fetchVolumeData:', error);
    throw error;
  }
}