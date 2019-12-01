export const getPageDescription = (
  pageSize,
  pageIndex,
  numRowsOnPage,
  numRowsTotal,
  canNextPage,
) => {
  const topRowIndex = pageSize * pageIndex + 1
  const bottomRowIndex = pageSize * pageIndex + numRowsOnPage
  let prefix = `${topRowIndex}-${bottomRowIndex}`

  if (!canNextPage) {
    if (numRowsOnPage === 1) {
      prefix = `${topRowIndex}`
    } else {
      prefix = `${topRowIndex}-${bottomRowIndex}`
    }
  }

  return `Jobs: ${prefix} of ${numRowsTotal}`
}
