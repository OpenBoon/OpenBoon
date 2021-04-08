import PropTypes from 'prop-types'

import { constants, spacing } from '../Styles'

import BackSvg from '../Icons/back.svg'

const ButtonExternal = ({ children }) => {
  return (
    <div
      css={{
        width: '100%',
        display: 'flex',
        justifyContent: 'space-between',
      }}
    >
      {children}

      <BackSvg
        height={constants.icons.regular}
        css={{
          transform: 'rotate(135deg)',
          opacity: constants.opacity.half,
          marginTop: -spacing.mini,
          marginRight: -spacing.small,
        }}
      />
    </div>
  )
}

ButtonExternal.propTypes = {
  children: PropTypes.node.isRequired,
}

export default ButtonExternal
