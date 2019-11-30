export const getPagination = (
  pageSize,
  pageIndex,
  numRowsPage,
  numRowsTotal,
  canNextPage,
) => {
  const topRowIndex = pageSize * pageIndex + 1
  const bottomRowIndex = pageSize * pageIndex + numRowsPage
  const prefix = !canNextPage
    ? `${topRowIndex}`
    : `${topRowIndex}-${bottomRowIndex}`
  return `Jobs: ${prefix} of ${numRowsTotal}`
}
