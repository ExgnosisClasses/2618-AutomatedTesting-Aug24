CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(50) PRIMARY KEY,
    account_type VARCHAR(20),
    balance DECIMAL(19, 2),
    status VARCHAR(20)
);

INSERT INTO accounts (account_number, account_type, balance, status) VALUES
    ('ACC-001', 'CHECKING', 1500.00, 'ACTIVE'),
    ('ACC-002', 'SAVINGS', 8200.50, 'ACTIVE'),
    ('ACC-003', 'CHECKING', 250.00, 'FROZEN');
