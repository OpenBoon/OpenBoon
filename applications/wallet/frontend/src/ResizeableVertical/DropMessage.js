import PropTypes from 'prop-types'

import { colors, typography } from '../Styles'

const TimelineDropMessage = ({ size, originSize }) => {
  return (
    <div
      css={{
        height: '100%',
        width: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        fontSize: typography.size.medium,
        fontHeight: typography.height.medium,
        fontWeight: typography.weight.medium,
        overflow: 'hidden',
        color: colors.structure.steel,
      }}
    >
      Release to {size < originSize ? 'collapse' : 'expand'}.
    </div>
  )
}

TimelineDropMessage.propTypes = {
  size: PropTypes.number.isRequired,
  originSize: PropTypes.number.isRequired,
}

export default TimelineDropMessage
