import PropTypes from 'prop-types'

import { typography } from '../Styles'

const TableEmpty = ({ numColumns, children }) => {
  return (
    <tr aria-label="Empty table" css={{ pointerEvents: 'none' }}>
      <td colSpan={numColumns}>
        <div
          css={{
            height: '300px',
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexDirection: 'column',
            color: 'red',
            fontSize: typography.size.kilo,
            lineHeight: typography.height.kilo,
          }}>
          {children}
        </div>
      </td>
    </tr>
  )
}

TableEmpty.propTypes = {
  numColumns: PropTypes.number.isRequired,
  children: PropTypes.node.isRequired,
}

export default TableEmpty
