import PropTypes from 'prop-types'

import TableException from './Exception'

const TableContent = ({
  numColumns,
  isLoading,
  results,
  renderEmpty,
  renderRow,
  revalidate,
}) => {
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
  isLoading: PropTypes.bool.isRequired,
  results: PropTypes.arrayOf(PropTypes.object).isRequired,
  renderEmpty: PropTypes.node.isRequired,
  renderRow: PropTypes.func.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default TableContent
