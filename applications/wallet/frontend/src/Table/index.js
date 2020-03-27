/* eslint-disable react/jsx-props-no-spreading */
import PropTypes from 'prop-types'

import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import TableContent from './Content'

const Table = ({ role, ...props }) => {
  return (
    <SuspenseBoundary role={role}>
      <TableContent {...props} />
    </SuspenseBoundary>
  )
}

Table.defaultProps = {
  role: null,
}

Table.propTypes = {
  role: PropTypes.oneOf(Object.keys(ROLES)),
}

export { Table as default, ROLES }
