import PropTypes from 'prop-types'
import { useState } from 'react'

import { colors, constants, spacing, zIndex } from '../Styles'

const ModelMatrixLabel = ({ cellDimension, label }) => {
  const [showLabel, setShowLabel] = useState(false)

  return (
    <div
      aria-label={`Prediction Label: ${label}`}
      onMouseEnter={() => setShowLabel(true)}
      onMouseLeave={() => setShowLabel(false)}
      css={{ position: 'relative', cursor: 'default' }}
    >
      <div
        css={{
          minWidth: cellDimension,
          maxWidth: cellDimension,
          padding: spacing.normal,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          borderRight: constants.borders.regular.coal,
        }}
      >
        {label}
      </div>
      {showLabel && (
        <div
          css={{
            position: 'absolute',
            top: 0,
            left: 0,
            height: '100%',
            minWidth: cellDimension,
            padding: spacing.normal,
            backgroundColor: colors.structure.lead,
            zIndex: zIndex.layout.interactive,
            boxShadow: constants.boxShadows.default,
          }}
        >
          {label}
        </div>
      )}
    </div>
  )
}

ModelMatrixLabel.propTypes = {
  cellDimension: PropTypes.number.isRequired,
  label: PropTypes.string.isRequired,
}

export default ModelMatrixLabel
