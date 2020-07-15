import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

const Tooltip = ({ children, content, style }) => {
  const [showPopup, setShowPopup] = useState(false)

  return (
    <div css={{ display: 'relative' }}>
      <div
        onFocus={() => setShowPopup(true)}
        onBlur={() => setShowPopup(false)}
        onMouseEnter={() => setShowPopup(true)}
        onMouseLeave={() => setShowPopup(false)}
      >
        {children}
      </div>

      {showPopup && (
        <div
          role="tooltip"
          aria-hidden={!showPopup}
          css={{
            position: 'absolute',
            zIndex: zIndex.reset,
            backgroundColor: colors.structure.iron,
            border: constants.borders.regular.steel,
            borderRadius: constants.borderRadius.small,
            padding: spacing.moderate,
            ...style,
          }}
        >
          {content}
        </div>
      )}
    </div>
  )
}

Tooltip.defaultProps = {
  style: {},
}

Tooltip.propTypes = {
  children: PropTypes.node.isRequired,
  content: PropTypes.node.isRequired,
  style: PropTypes.shape({
    name: PropTypes.string,
    styles: PropTypes.string,
  }),
}

export default Tooltip
