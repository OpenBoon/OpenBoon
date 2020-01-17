import PropTypes from 'prop-types'

import { colors, typography, constants, spacing } from '../Styles'

const TD_PADDING = spacing.base * 2
const NBSP_HEIGHT = spacing.normal

const TableException = ({ numColumns, children }) => {
  return (
    <tr css={{ pointerEvents: 'none' }}>
      <td colSpan={numColumns}>
        <div
          css={{
            height: `calc(100vh - ${constants.navbar.height +
              constants.pageTitle.height +
              constants.tableHeader.height +
              TD_PADDING +
              NBSP_HEIGHT}px)`,
            width: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            color: colors.structure.steel,
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
