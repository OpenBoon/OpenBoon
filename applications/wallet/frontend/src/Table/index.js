/* eslint-disable react/jsx-props-no-spreading */
import SuspenseBoundary from '../SuspenseBoundary'

import TableContent from './Content'

const Table = props => {
  return (
    <SuspenseBoundary>
      <TableContent {...props} />
    </SuspenseBoundary>
  )
}

export default Table
