import axios from 'axios';

// API base URL - adjust based on how your Spring Boot app is configured
// If you're using proxy in package.json, leave this empty
const API_BASE_URL = '';

// Spring Boot controller context path
const API_CONTEXT_PATH = '/api/wallet-analysis';

// Data source tracking
export let walletDataSource = 'loading'; // 'api', 'simulator', 'loading', or 'error'

// Types
export type Counterparty = {
  address: string;
  name: string;
  type: 'wallet' | 'contract' | 'protocol' | 'exchange';
  transactionCount: number;
  protocol?: string;
};

export type WalletAnalysisData = {
  walletAddress: string;
  topCounterparties: Counterparty[];
};

// Protocol lookup - this would be expanded in a real implementation
const KNOWN_PROTOCOLS: Record<string, { name: string, type: 'protocol' | 'exchange' }> = {
  '0x68b3465833fb72a70ecdf485e0e4c7bd8665fc45': { name: 'Uniswap Router', type: 'protocol' },
  '0xdef1c0ded9bec7f1a1670819833240f027b25eff': { name: 'Coinbase', type: 'exchange' },
  '0x6c3f90f043a72fa612cbac8115ee7e52bde6e490': { name: 'Curve 3Pool', type: 'protocol' },
  '0xf7ba25e4e99c2e2d6603e2fc40743f5a525165fc': { name: 'Aerodrome Router', type: 'protocol' },
  '0x4c36d2919e407f0cc2ee3c993ccf8ac26d9ce64e': { name: 'Base Bridge', type: 'protocol' }
  // Add more known protocols as needed
};

// Fetch wallet analysis data from backend API
export async function fetchWalletAnalysis(walletAddress: string): Promise<WalletAnalysisData> {
  // Update data source
  walletDataSource = 'loading';

  try {
    // Validate wallet address
    if (!walletAddress || !walletAddress.startsWith('0x') || walletAddress.length !== 42) {
      throw new Error('Invalid wallet address');
    }

    const apiUrl = `${API_BASE_URL}${API_CONTEXT_PATH}/counterparties?address=${walletAddress}`;

    console.log(`Fetching wallet analysis from: ${apiUrl}`);

    const response = await axios.get(apiUrl);

    console.log('Wallet API Response:', response.data);

    // Check if response data is valid
    if (!response.data || !response.data.topCounterparties) {
      console.warn('Invalid response format from API:', response.data);
      walletDataSource = 'simulator';
      return generateMockWalletData(walletAddress);
    }

    // Set data source to API
    walletDataSource = 'api';

    return response.data;
  } catch (error) {
    console.error('Error fetching wallet analysis:', error);

    // Set data source to simulator due to error
    walletDataSource = 'error';

    // Fall back to mock data
    return generateMockWalletData(walletAddress);
  }
}

// Generate mock data for development/fallback
function generateMockWalletData(walletAddress: string): WalletAnalysisData {
  console.log(`Generating mock data for wallet: ${walletAddress}`);

  // Create mock counterparties
  const mockCounterparties: Counterparty[] = [
    {
      address: '0x68b3465833fb72a70ecdf485e0e4c7bd8665fc45',
      name: 'Uniswap Router',
      type: 'protocol',
      transactionCount: Math.floor(10 + Math.random() * 40),
      protocol: 'Uniswap'
    },
    {
      address: '0xdef1c0ded9bec7f1a1670819833240f027b25eff',
      name: 'Coinbase',
      type: 'exchange',
      transactionCount: Math.floor(5 + Math.random() * 30),
      protocol: 'Coinbase'
    },
    {
      address: '0x' + Array(40).fill(0).map(() =>
        Math.floor(Math.random() * 16).toString(16)).join(''),
      name: 'Unknown Wallet',
      type: 'wallet',
      transactionCount: Math.floor(2 + Math.random() * 20),
    },
    {
      address: '0x6c3f90f043a72fa612cbac8115ee7e52bde6e490',
      name: 'Curve 3Pool',
      type: 'protocol',
      transactionCount: Math.floor(2 + Math.random() * 15),
      protocol: 'Curve'
    },
    {
      address: '0x' + Array(40).fill(0).map(() =>
        Math.floor(Math.random() * 16).toString(16)).join(''),
      name: 'Contract',
      type: 'contract',
      transactionCount: Math.floor(1 + Math.random() * 10),
    },
    {
      address: '0x4c36d2919e407f0cc2ee3c993ccf8ac26d9ce64e',
      name: 'Base Bridge',
      type: 'protocol',
      transactionCount: Math.floor(1 + Math.random() * 8),
      protocol: 'Base Bridge'
    },
    {
      address: '0x' + Array(40).fill(0).map(() =>
        Math.floor(Math.random() * 16).toString(16)).join(''),
      name: 'Unknown Wallet',
      type: 'wallet',
      transactionCount: Math.floor(1 + Math.random() * 6),
    },
    {
      address: '0x' + Array(40).fill(0).map(() =>
        Math.floor(Math.random() * 16).toString(16)).join(''),
      name: 'Contract',
      type: 'contract',
      transactionCount: Math.floor(1 + Math.random() * 5),
    },
    {
      address: '0x' + Array(40).fill(0).map(() =>
        Math.floor(Math.random() * 16).toString(16)).join(''),
      name: 'Unknown Wallet',
      type: 'wallet',
      transactionCount: Math.floor(1 + Math.random() * 4),
    },
    {
      address: '0x' + Array(40).fill(0).map(() =>
        Math.floor(Math.random() * 16).toString(16)).join(''),
      name: 'Unknown Wallet',
      type: 'wallet',
      transactionCount: Math.floor(1 + Math.random() * 3),
    }
  ];

  return {
    walletAddress,
    topCounterparties: mockCounterparties
  };
}

// Helper function to identify protocol from address
export function identifyProtocol(address: string): { name: string, type: 'protocol' | 'exchange' | 'wallet' | 'contract' } {
  const lowerCaseAddress = address.toLowerCase();

  // Check if address is in known protocols
  if (KNOWN_PROTOCOLS[lowerCaseAddress]) {
    return KNOWN_PROTOCOLS[lowerCaseAddress];
  }

  // Default to unknown contract (in a real implementation,
  // you would use ethers.js to check if address is a contract)
  return { name: 'Unknown', type: 'contract' };
}

// Format wallet address for display (0x1234...5678)
export function formatAddress(address: string): string {
  if (!address || address.length < 10) return address;

  return `${address.substring(0, 6)}...${address.substring(address.length - 4)}`;
}