-- Create the portfolio database if it does not exist
SELECT 'CREATE DATABASE portfolio'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'portfolio'
)\gexec

-- Create the marketdata database if it does not exist
SELECT 'CREATE DATABASE marketdata'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'marketdata'
)\gexec

-- Create the portfolio-svc user if it does not exist
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'portfolio-svc') THEN
            CREATE ROLE "portfolio-svc" WITH LOGIN PASSWORD 'portfolio-pass';
        END IF;
    END
$$;

-- Create the market-svc user if it does not exist
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'market-svc') THEN
            CREATE ROLE "market-svc" WITH LOGIN PASSWORD 'market-pass';
        END IF;
    END
$$;

-- Switch to the portfolio database
\c portfolio admin

-- Create custom schema for portfolio database
CREATE SCHEMA IF NOT EXISTS portfolio_schema AUTHORIZATION "portfolio-svc";

-- Revoke public access and assign permissions to portfolio-svc
REVOKE ALL ON SCHEMA portfolio_schema FROM PUBLIC;
GRANT USAGE ON SCHEMA portfolio_schema TO "portfolio-svc";
GRANT CREATE ON SCHEMA portfolio_schema TO "portfolio-svc";
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA portfolio_schema TO "portfolio-svc";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA portfolio_schema TO "portfolio-svc";

-- Set default privileges for portfolio_schema
ALTER DEFAULT PRIVILEGES IN SCHEMA portfolio_schema
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "portfolio-svc";
ALTER DEFAULT PRIVILEGES IN SCHEMA portfolio_schema
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO "portfolio-svc";

-- Switch to the marketdata database
\c marketdata admin

-- Create custom schema for marketdata database
CREATE SCHEMA IF NOT EXISTS marketdata_schema AUTHORIZATION "market-svc";

-- Revoke public access and assign permissions to market-svc
REVOKE ALL ON SCHEMA marketdata_schema FROM PUBLIC;
GRANT USAGE ON SCHEMA marketdata_schema TO "market-svc";
GRANT CREATE ON SCHEMA marketdata_schema TO "market-svc";
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA marketdata_schema TO "market-svc";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA marketdata_schema TO "market-svc";

-- Set default privileges for marketdata_schema
ALTER DEFAULT PRIVILEGES IN SCHEMA marketdata_schema
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "market-svc";
ALTER DEFAULT PRIVILEGES IN SCHEMA marketdata_schema
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO "market-svc";