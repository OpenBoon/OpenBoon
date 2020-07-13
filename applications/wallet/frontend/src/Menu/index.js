import { useRef, useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, zIndex, spacing } from '../Styles'

import { onBlur as onBlurHelper } from './helpers'

export const WIDTH = 200

const Menu = ({ button, children, open }) => {
  const container = useRef(null)

  const [isMenuOpen, setMenuOpen] = useState(false)

  const onBlur = onBlurHelper({ container, setMenuOpen })
  const onClick = () => setMenuOpen(!isMenuOpen)

  return (
    <div ref={container} css={{ position: 'relative' }}>
      {button({ onBlur, onClick, isMenuOpen })}
      {isMenuOpen && (
        <div
          css={{
            position: 'absolute',
            zIndex: zIndex.reset,
            top: '100%',
            [open === 'left' ? 'right' : 'left']: 0,
            backgroundColor: colors.structure.steel,
            borderRadius: constants.borderRadius.small,
            boxShadow: constants.boxShadows.menu,
            minWidth: WIDTH,
            ul: {
              listStyleType: 'none',
              padding: 0,
              paddingTop: spacing.small,
              paddingBottom: spacing.small,
              margin: 0,
            },
            li: { display: 'flex' },
          }}
        >
          {children({ onBlur, onClick, isMenuOpen })}
        </div>
      )}
    </div>
  )
}

Menu.propTypes = {
  open: PropTypes.oneOf(['left', 'right']).isRequired,
  button: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
}

export default Menu
