import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

const ResizeableDropMessage = ({ size, originSize, isHorizontal }) => {
  return (
    <div
      css={{
        [isHorizontal ? 'height' : 'width']: '100%',
        [isHorizontal ? 'width' : 'height']: size,
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        fontSize: typography.size.medium,
        lineHeight: typography.height.medium,
        fontWeight: typography.weight.medium,
        overflow: 'hidden',
        color: colors.structure.steel,
        padding: spacing.normal,
        whiteSpace: 'nowrap',
      }}
    >
      Release to {size < originSize ? 'collapse' : 'expand'}.
    </div>
  )
}

ResizeableDropMessage.propTypes = {
  size: PropTypes.number.isRequired,
  originSize: PropTypes.number.isRequired,
  isHorizontal: PropTypes.bool.isRequired,
}

export default ResizeableDropMessage
