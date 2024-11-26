export async function fetchTransactions(currentPage: number) {
  const url = `http://localhost:8080/v1/stocks/transactions?userId=user&currency=USD&page=${currentPage}`;

  let transactions = await fetch(url).then((res) => res.json());

  console.log(transactions);
  return transactions;
}
