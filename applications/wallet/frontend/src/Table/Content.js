import PropTypes from 'prop-types'

import TableException from './Exception'

const TableContent = ({
  numColumns,
  hasError,
  isLoading,
  results,
  renderEmpty,
  renderRow,
  revalidate,
}) => {
  if (hasError) {
    return (
      <TableException numColumns={numColumns}>
        Hmmm, something went wrong.
        <br /> Please try refreshing.
      </TableException>
    )
  }

  if (isLoading) {
    return <TableException numColumns={numColumns}>Loading...</TableException>
  }

  if (results.length === 0) {
    return (
      <TableException numColumns={numColumns}>{renderEmpty}</TableException>
    )
  }

  return results.map(result => renderRow({ result, revalidate }))
}

TableContent.propTypes = {
  numColumns: PropTypes.number.isRequired,
  hasError: PropTypes.bool.isRequired,
  isLoading: PropTypes.bool.isRequired,
  results: PropTypes.arrayOf(PropTypes.object).isRequired,
  renderEmpty: PropTypes.node.isRequired,
  renderRow: PropTypes.func.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default TableContent
