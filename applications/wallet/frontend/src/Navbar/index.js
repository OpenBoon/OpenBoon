import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing, constants, zIndex } from '../Styles'

import LogoSvg from '../Icons/logo.svg'
import HamburgerSvg from '../Icons/hamburger.svg'

import ProjectSwitcher from '../ProjectSwitcher'

const LOGO_HEIGHT = 18

const Navbar = ({ projectId, isSidebarOpen, setSidebarOpen, children }) => {
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
      }}
    >
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
              marginLeft: -spacing.base,
              cursor: 'pointer',
            }}
          >
            <HamburgerSvg height={constants.icons.regular} />
          </button>
        )}

        <Link href="/" passHref>
          <a
            css={{
              paddingLeft: spacing.base,
              paddingRight: spacing.base,
              paddingBottom: spacing.mini,
              display: 'flex',
              alignItems: 'center',
            }}
            aria-label="Home"
          >
            <LogoSvg height={LOGO_HEIGHT} />
          </a>
        </Link>

        <ProjectSwitcher projectId={projectId} />
      </div>

      {children}
    </div>
  )
}

Navbar.propTypes = {
  projectId: PropTypes.string.isRequired,
  isSidebarOpen: PropTypes.bool.isRequired,
  setSidebarOpen: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default Navbar
