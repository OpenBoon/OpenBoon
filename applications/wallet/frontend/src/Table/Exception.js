import PropTypes from 'prop-types'

import { colors, typography } from '../Styles'

const TableException = ({ numColumns, children }) => {
  return (
    <tr css={{ height: '100%', pointerEvents: 'none' }}>
      <td colSpan={numColumns}>
        <div
          css={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            color: colors.structure.steel,
            fontSize: typography.size.hecto,
            lineHeight: typography.height.hecto,
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
