export const getPageDescription = ({
  pageSize,
  pageIndex,
  numRowsOnPage,
  numRowsTotal,
  canNextPage,
}) => {
  const topRowIndex = pageSize * pageIndex + 1
  const bottomRowIndex = pageSize * pageIndex + numRowsOnPage

  if (!canNextPage && numRowsOnPage === 1) {
    return `Jobs: ${topRowIndex} of ${numRowsTotal}`
  }
  return `Jobs: ${topRowIndex}-${bottomRowIndex} of ${numRowsTotal}`
}
