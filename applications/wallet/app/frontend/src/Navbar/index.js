import PropTypes from 'prop-types'

import userShape from '../User/shape'

import { colors, spacing, constants, zIndex } from '../Styles'

import LogoSvg from '../Icons/logo.svg'

import ProjectSwitcher from '../ProjectSwitcher'
import UserMenu from '../UserMenu'

import HamburgerSvg from './hamburger.svg'

const LOGO_WIDTH = 110

const Navbar = ({ user, isSidebarOpen, setSidebarOpen, logout }) => {
  return (
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
        backgroundColor: colors.grey1,
        boxShadow: constants.boxShadows.navBar,
        zIndex: zIndex.layout.navbar,
        paddingLeft: spacing.normal,
        paddingRight: spacing.normal,
      }}>
      <div css={{ display: 'flex', alignItems: 'stretch' }}>
        <button
          aria-label="Open Sidebar Menu"
          type="button"
          onClick={() => setSidebarOpen(!isSidebarOpen)}
          css={{
            border: 0,
            backgroundColor: 'inherit',
            color: colors.structure.steel,
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            padding: spacing.base,
            margin: 0,
            marginLeft: -spacing.base,
            cursor: 'pointer',
          }}>
          <HamburgerSvg width={20} aria-hidden />
        </button>

        <div css={{ paddingLeft: spacing.base, paddingRight: spacing.base }}>
          <LogoSvg width={LOGO_WIDTH} />
        </div>

        <ProjectSwitcher />
      </div>

      <UserMenu user={user} logout={logout} />
    </div>
  )
}

Navbar.propTypes = {
  user: PropTypes.shape(userShape).isRequired,
  isSidebarOpen: PropTypes.bool.isRequired,
  setSidebarOpen: PropTypes.func.isRequired,
  logout: PropTypes.func.isRequired,
}

export default Navbar
