import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import { colors, spacing, constants, zIndex } from '../Styles'

import LogoSvg from '../Icons/logo.svg'

import ProjectSwitcher from '../ProjectSwitcher'

import HamburgerSvg from './hamburger.svg'

const LOGO_WIDTH = 110

const Navbar = ({ isSidebarOpen, setSidebarOpen, children }) => {
  const {
    query: { projectId },
  } = useRouter()
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
        backgroundColor: colors.structure.mattGrey,
        boxShadow: constants.boxShadows.navBar,
        zIndex: zIndex.layout.navbar,
        paddingLeft: spacing.normal,
        paddingRight: spacing.normal,
      }}>
      <div css={{ display: 'flex', alignItems: 'stretch' }}>
        {!!projectId && (
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
        )}

        <div css={{ paddingLeft: spacing.base, paddingRight: spacing.base }}>
          <LogoSvg width={LOGO_WIDTH} />
        </div>

        <ProjectSwitcher />
      </div>

      {children}
    </div>
  )
}

Navbar.propTypes = {
  isSidebarOpen: PropTypes.bool.isRequired,
  setSidebarOpen: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default Navbar
