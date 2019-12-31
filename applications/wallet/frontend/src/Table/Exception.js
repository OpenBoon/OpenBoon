import PropTypes from 'prop-types'

import { colors, typography } from '../Styles'

const HEIGHT = 300

const TableException = ({ numColumns, children }) => {
  return (
    <tr css={{ pointerEvents: 'none' }}>
      <td colSpan={numColumns}>
        <div
          css={{
            height: HEIGHT,
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexDirection: 'column',
            color: colors.signal.warning.base,
            fontSize: typography.size.kilo,
            lineHeight: typography.height.kilo,
          }}>
          {children}
        </div>
      </td>
    </tr>
  )
}

TableException.propTypes = {
  numColumns: PropTypes.number.isRequired,
  children: PropTypes.node.isRequired,
}

export default TableException
