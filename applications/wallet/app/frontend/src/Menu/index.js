import { useRef, useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, zIndex, spacing } from '../Styles'

import { onBlur as onBlurHelper } from './helpers'

const WIDTH = 200

const Menu = ({ button, children }) => {
  const container = useRef(null)

  const [isMenuOpen, setMenuOpen] = useState(false)

  const onBlur = onBlurHelper({ container, setMenuOpen })
  const onClick = () => setMenuOpen(!isMenuOpen)

  return (
    <div ref={container} css={{ position: 'relative' }}>
      {button({ onBlur, onClick })}
      {isMenuOpen && (
        <div
          css={{
            position: 'absolute',
            zIndex: zIndex.reset,
            top: '100%',
            right: 0,
            backgroundColor: colors.rocks.iron,
            borderRadius: constants.borderRadius.small,
            boxShadow: constants.boxShadows.menu,
            width: WIDTH,
            ul: {
              listStyleType: 'none',
              padding: 0,
              paddingTop: spacing.small,
              paddingBottom: spacing.small,
              margin: 0,
            },
            li: { display: 'flex' },
          }}>
          {children({ onBlur, onClick })}
        </div>
      )}
    </div>
  )
}

Menu.propTypes = {
  button: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
}

export default Menu
