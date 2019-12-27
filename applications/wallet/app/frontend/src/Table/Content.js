import PropTypes from 'prop-types'

import TableEmpty from './Empty'

const TableContent = ({
  numColumns,
  isLoading,
  results,
  renderEmpty,
  renderRow,
  revalidate,
}) => {
  if (isLoading) return 'Loading...'

  if (results.length === 0) {
    return <TableEmpty numColumns={numColumns}>{renderEmpty}</TableEmpty>
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
