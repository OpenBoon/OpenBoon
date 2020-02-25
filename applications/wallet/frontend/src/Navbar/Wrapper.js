import PropTypes from 'prop-types'

import { colors, spacing, constants, zIndex } from '../Styles'

const NavbarWrapper = ({ children, style }) => (
  <div
    css={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      height: constants.navbar.height,
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      backgroundColor: colors.structure.mattGrey,
      boxShadow: constants.boxShadows.navBar,
      zIndex: zIndex.layout.navbar,
      paddingLeft: spacing.normal,
      paddingRight: spacing.normal,
      ...style,
    }}>
    {children}
  </div>
)

NavbarWrapper.defaultProps = {
  style: {},
}

NavbarWrapper.propTypes = {
  children: PropTypes.node.isRequired,
  style: PropTypes.shape({ name: PropTypes.string, styles: PropTypes.string }),
}

export default NavbarWrapper
