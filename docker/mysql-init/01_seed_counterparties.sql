-- 01_schema_and_seed.sql

-- ─── 1) COUNTERPARTY_INFO ───────────────────────────────────────────────────
USE cardload;
CREATE TABLE IF NOT EXISTS counterparty_info (
    address           VARCHAR(42)    NOT NULL PRIMARY KEY,
    transaction_count INT            NOT NULL,
    name              VARCHAR(255),
    type              VARCHAR(50),
    protocol          VARCHAR(255),
    is_known_entity   BOOLEAN        NOT NULL,
    etherscan_url     VARCHAR(512)
);

-- ─── 2) TOKEN_TRANSFER ─────────────────────────────────────────────────────

-- CREATE to match your @Entity fields
CREATE TABLE token_transfers (
    tx_hash        VARCHAR(100)    NOT NULL,
    token_address  VARCHAR(42)     NOT NULL,
    token_symbol   VARCHAR(50)     NOT NULL,
    from_address   VARCHAR(42)     NOT NULL,
    amount         DECIMAL(38,0)   NOT NULL,
    usd_value      DECIMAL(30,10)  NOT NULL,
    `timestamp`    DATETIME        NOT NULL,
    decimals       INT             NOT NULL,
    PRIMARY KEY (tx_hash)
);


-- ─── 3) SEED COUNTERPARTIES ─────────────────────────────────────────────────

INSERT INTO counterparty_info
  (address, transaction_count, name, type, protocol, is_known_entity, etherscan_url)
VALUES
  ('0x28c6c06298d514db089934071355e5743bf21d60', 0, 'Binance',            'EXCHANGE', 'Binance',           TRUE, NULL),
  ('0xddfabcdc4d8ffc6d5beaf154f18b778f892a0740', 0, 'Coinbase',           'EXCHANGE', 'Coinbase',          TRUE, NULL),
  ('0x881d40237659c251811cec9c364ef91dc08d300c', 0, 'Kraken',             'EXCHANGE', 'Kraken',            TRUE, NULL),
  ('0x4cdf24e0584985c94879e85cd6ac5e25f31d5eca', 0, 'Aerodrome Router',   'PROTOCOL', 'Aerodrome Finance', TRUE, NULL),
  ('0xc5af84701f98fa483ece78af83f11b6c38aca71d', 0, 'Base Bridge',        'PROTOCOL', 'Base Bridge',       TRUE, NULL),
  ('0xbbbc1f6be7b76a15b3532a2b27d26c8ca96eade8', 0, 'Uniswap V3 Router',  'PROTOCOL', 'Uniswap',           TRUE, NULL),
  ('0xc30141b657f4216252dc59af2e7cdb9d8792e1b0', 0, 'Swap Router',        'PROTOCOL', 'Uniswap',           TRUE, NULL),
  ('0xcccd218a58b53c67fc17d8c87cb90d83614e35fd', 0, 'Cypher Master Wallet','WALLET',  NULL,                TRUE, NULL);
