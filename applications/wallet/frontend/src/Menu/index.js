import { useRef, useState } from 'react'
import PropTypes from 'prop-types'

import stylesShape from '../Styles/shape'

import { colors, constants, zIndex, spacing, typography } from '../Styles'

import { onBlur as onBlurHelper } from './helpers'

export const WIDTH = 200

const Menu = ({ button, children, open, style }) => {
  const container = useRef(null)

  const [isMenuOpen, setMenuOpen] = useState(false)

  const onBlur = onBlurHelper({ container, setMenuOpen })
  const onClick = () => setMenuOpen(!isMenuOpen)

  return (
    <div ref={container} css={{ position: 'relative', height: '100%' }}>
      {button({ onBlur, onClick, isMenuOpen, style })}
      {isMenuOpen && (
        <div
          css={{
            position: 'absolute',
            zIndex: zIndex.reset,
            [open.includes('bottom') ? 'top' : 'bottom']: '100%',
            right: open.includes('center') ? 0 : 'auto',
            [open.includes('left') ? 'right' : 'left']: 0,
            backgroundColor: colors.structure.steel,
            borderRadius: constants.borderRadius.small,
            boxShadow: constants.boxShadows.menu,
            minWidth: WIDTH,
            fontFamily: typography.family.regular,
            ul: {
              maxHeight: '50vh',
              overflow: 'scroll',
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

Menu.defaultProps = {
  style: {},
}

Menu.propTypes = {
  open: PropTypes.oneOf([
    'top-left',
    'top-right',
    'bottom-left',
    'bottom-right',
    'bottom-center',
  ]).isRequired,
  button: PropTypes.func.isRequired,
  style: stylesShape,
  children: PropTypes.func.isRequired,
}

export default Menu
