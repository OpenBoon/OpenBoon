import PropTypes from 'prop-types'

import { colors, typography } from '../Styles'

const ResizeableVerticalDialog = ({ size, startingSize, minExpandedSize }) => {
  if (size >= minExpandedSize) return null

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
      Release to {size < startingSize ? 'collapse' : 'expand'}.
    </div>
  )
}

ResizeableVerticalDialog.propTypes = {
  size: PropTypes.number.isRequired,
  startingSize: PropTypes.number.isRequired,
  minExpandedSize: PropTypes.number.isRequired,
}

export default ResizeableVerticalDialog
